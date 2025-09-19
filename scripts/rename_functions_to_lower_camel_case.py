#!/usr/bin/env python3
"""Rename Kotlin function declarations across the project to lowerCamelCase.

The script scans all tracked Kotlin source files, converts function declaration
names to lower camel case when they do not already conform, and updates call
sites and function references accordingly.

It intentionally skips overriding functions (those marked with ``override``)
so that it does not break interface or superclass contracts. When possible, it
also avoids conflicting renames (e.g., when the target name already exists with
proper casing).

Usage
-----
Run the script from the project root:

    python scripts/rename_functions_to_lower_camel_case.py --dry-run

    # Apply the changes in place
    python scripts/rename_functions_to_lower_camel_case.py --apply

By default the script performs a dry run and prints the planned renames and
files that would change. Review the output carefully before running with
``--apply``.

This utility relies on heuristics and regular expressions — always review the
resulting diff after running it, especially for complex language features or
metaprogramming scenarios.
"""

from __future__ import annotations

import argparse
import dataclasses
import re
import subprocess
from collections import defaultdict
from pathlib import Path
from typing import Dict, Iterable, List, Sequence, Tuple

# Regex used to find Kotlin function declarations. The pattern captures
# optional annotations and modifiers, along with the function name so that we
# can later decide whether we should rename it.
FUN_DECL_PATTERN = re.compile(
    r"""
    (?P<prefix>
        (?:@[A-Za-z0-9_.]+(?:\([^)]*\))?\s*)*            # annotations
        (?:\b(?:public|private|internal|protected|final|open|abstract|
                sealed|const|external|inline|tailrec|operator|infix|suspend|
                override|crossinline|noinline|vararg|reified|lateinit|data|
                inner|enum|annotation|companion|value|expect|actual)
           \b\s*)*
    )
    fun\s+                                                  # fun keyword
    (?P<generics><[^>{}]*>\s*)?                             # optional generics
    (?P<receiver>(?:[A-Za-z0-9_?.<>,\s]+\s*\.\s*)*)      # optional receiver chain
    (?P<name>[A-Za-z_][A-Za-z0-9_]*)                        # function name
    (?P<rest>\s*\()                                       # opening parenthesis
    """,
    re.MULTILINE | re.VERBOSE,
)

# Pattern to determine whether an identifier already follows lowerCamelCase.
LOWER_CAMEL_RE = re.compile(r"^[a-z][A-Za-z0-9]*$")

# Patterns used to update call sites and function references. These ensure that
# we only rename invocations or references without touching unrelated symbols.
CALL_SITE_PATTERNS = (
    # Direct calls, possibly with generic arguments: FooBar(), FooBar<Int>()
    r"\b{old}\b(?=\s*(?:<[^>]*>)?\s*\()",
    # Function references, including bound references: ::FooBar, this::FooBar
    r"(::\s*){old}\b",
    # Import statements for top-level functions: import pkg.FooBar
    r"(import\s+[A-Za-z0-9_.]*\.){old}\b",
)


@dataclasses.dataclass
class FunctionOccurrence:
    """Represents a discovered function declaration in the code base."""

    file_path: Path
    original_name: str
    suggested_name: str
    is_override: bool
    line_number: int


def git_tracked_kotlin_files(root: Path) -> List[Path]:
    """Return Kotlin source files tracked by git under the given root."""

    result = subprocess.run(
        ["git", "ls-files", "*.kt"],
        cwd=root,
        check=True,
        text=True,
        capture_output=True,
    )
    return [root / Path(line.strip()) for line in result.stdout.splitlines() if line.strip()]


def discover_functions(files: Sequence[Path]) -> List[FunctionOccurrence]:
    """Scan all provided files and gather function declaration metadata."""

    occurrences: List[FunctionOccurrence] = []

    for file_path in files:
        content = file_path.read_text(encoding="utf-8")
        for match in FUN_DECL_PATTERN.finditer(content):
            name = match.group("name")
            if "`" in name:  # Ignore backtick-escaped identifiers.
                continue

            prefix = match.group("prefix") or ""
            is_override = bool(re.search(r"\\boverride\\b", prefix))

            if is_override:
                continue

            if LOWER_CAMEL_RE.match(name):
                continue

            suggested = to_lower_camel(name)
            if suggested == name:
                continue

            line_number = content.count("\n", 0, match.start("name")) + 1
            occurrences.append(
                FunctionOccurrence(
                    file_path=file_path,
                    original_name=name,
                    suggested_name=suggested,
                    is_override=is_override,
                    line_number=line_number,
                )
            )

    return occurrences


def to_lower_camel(name: str) -> str:
    """Convert an identifier to lowerCamelCase using best-effort heuristics."""

    if not name:
        return name

    # Split on underscores first.
    parts: List[str] = []
    for chunk in name.split("_"):
        if not chunk:
            continue
        parts.extend(split_camel_case(chunk))

    if not parts:
        parts = split_camel_case(name)

    if not parts:
        return name

    first, *rest = parts
    normalized = [first.lower()]
    normalized.extend(token.title() for token in rest)
    candidate = "".join(normalized)
    return candidate


def split_camel_case(identifier: str) -> List[str]:
    """Split an identifier into constituent words based on case transitions."""

    tokens = re.findall(r"[A-Z]+(?=[A-Z][a-z0-9]|$)|[A-Z]?[a-z0-9]+", identifier)
    if not tokens:
        return [identifier]
    return [token.lower() for token in tokens]


def build_rename_plan(occurrences: Sequence[FunctionOccurrence]) -> Tuple[Dict[str, str], Dict[str, List[FunctionOccurrence]]]:
    """Determine which function names should be renamed and detect conflicts."""

    rename_map: Dict[str, str] = {}
    conflicts: Dict[str, List[FunctionOccurrence]] = defaultdict(list)
    existing_names: Dict[str, List[FunctionOccurrence]] = defaultdict(list)

    for occ in occurrences:
        existing_names[occ.original_name].append(occ)

    for occ in occurrences:
        old = occ.original_name
        new = occ.suggested_name

        rename_map.setdefault(old, new)

    # Detect cases where different original names map to the same target.
    inverted: Dict[str, str] = {}
    for old, new in rename_map.items():
        if new in inverted and inverted[new] != old:
            # Conflict: two distinct function names map to the same target.
            conflicts[new].append(existing_names[old][0])
            conflicts[new].append(existing_names[inverted[new]][0])
        else:
            inverted[new] = old

    # Remove conflicting renames from the plan.
    for conflicted_name in conflicts:
        conflicting_originals = {occ.original_name for occ in conflicts[conflicted_name]}
        for original in conflicting_originals:
            rename_map.pop(original, None)

    return rename_map, conflicts


def apply_renames_to_content(content: str, rename_map: Dict[str, str]) -> Tuple[str, bool]:
    """Return updated content with declarations and call sites renamed."""

    changed = False

    def replace_declaration(match: re.Match[str]) -> str:
        nonlocal changed
        name = match.group("name")
        replacement = rename_map.get(name)
        if not replacement:
            return match.group(0)
        changed = True
        return (
            f"{match.group('prefix')}fun "
            f"{match.group('generics') or ''}"
            f"{match.group('receiver') or ''}"
            f"{replacement}"
            f"{match.group('rest')}"
        )

    content_after_decls = FUN_DECL_PATTERN.sub(replace_declaration, content)

    # Update call sites and references.
    content_after_calls = content_after_decls
    for old, new in rename_map.items():
        if old == new:
            continue
        for pattern in CALL_SITE_PATTERNS:
            regex = re.compile(pattern.format(old=re.escape(old)))
            def _replacer(match: re.Match[str]) -> str:
                nonlocal changed
                changed = True
                if match.lastindex:
                    # Keep the first captured group (e.g., leading '::' or '.').
                    groups = list(match.groups())
                    groups.append(new)
                    return "".join(groups)
                return new

            content_after_calls = regex.sub(_replacer, content_after_calls)

    return content_after_calls, changed


def rewrite_files(files: Iterable[Path], rename_map: Dict[str, str], apply_changes: bool) -> List[Path]:
    """Rewrite files with the provided renames. Returns files that change."""

    updated_files: List[Path] = []

    for file_path in files:
        content = file_path.read_text(encoding="utf-8")
        new_content, changed = apply_renames_to_content(content, rename_map)
        if not changed:
            continue
        updated_files.append(file_path)
        if apply_changes:
            file_path.write_text(new_content, encoding="utf-8")

    return updated_files


def print_summary(rename_map: Dict[str, str], conflicts: Dict[str, List[FunctionOccurrence]], updated_files: Sequence[Path], apply_changes: bool) -> None:
    """Pretty-print the result of the rename operation."""

    if rename_map:
        print("Planned renames:")
        for old, new in sorted(rename_map.items()):
            print(f"  {old} -> {new}")
    else:
        print("No rename candidates found.")

    if conflicts:
        print("\nConflicts detected (skipped):")
        for target, occs in conflicts.items():
            origins = {occ.original_name for occ in occs}
            print(f"  Target '{target}' blocked by: {', '.join(sorted(origins))}")

    if updated_files:
        mode = "Updated" if apply_changes else "Would update"
        print(f"\n{mode} {len(updated_files)} file(s):")
        for path in sorted(updated_files):
            print(f"  {path}")
    elif rename_map:
        print("\nNo files required changes; rename map may have been empty after conflicts.")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Apply the changes in place. Without this flag a dry run is performed.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Explicitly request a dry run (default).",
    )
    parser.add_argument(
        "--root",
        type=Path,
        default=Path.cwd(),
        help="Project root. Defaults to the current working directory.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    apply_changes = args.apply and not args.dry_run

    root = args.root.resolve()
    if not root.exists():
        raise SystemExit(f"Root path '{root}' does not exist.")

    kotlin_files = git_tracked_kotlin_files(root)
    if not kotlin_files:
        print("No Kotlin files found in the repository.")
        return

    occurrences = discover_functions(kotlin_files)
    rename_map, conflicts = build_rename_plan(occurrences)

    updated_files = rewrite_files(kotlin_files, rename_map, apply_changes)
    print_summary(rename_map, conflicts, updated_files, apply_changes)

    if not apply_changes:
        print("\nDry run complete. Re-run with --apply to write the changes.")


if __name__ == "__main__":
    main()

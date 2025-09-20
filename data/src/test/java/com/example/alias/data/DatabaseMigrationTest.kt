package com.example.alias.data

import com.example.alias.data.db.ALL_MIGRATIONS
import com.example.alias.data.db.MIGRATION_1_2
import com.example.alias.data.db.MIGRATION_2_3
import com.example.alias.data.db.MIGRATION_3_4
import com.example.alias.data.db.MIGRATION_4_5
import com.example.alias.data.db.MIGRATION_5_6
import com.example.alias.data.db.MIGRATION_6_7
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DatabaseMigrationTest {

    @Test
    fun migrationObjectsAreCreated() {
        // Test that all migration objects are properly instantiated
        assertNotNull("MIGRATION_1_2 should not be null", MIGRATION_1_2)
        assertNotNull("MIGRATION_2_3 should not be null", MIGRATION_2_3)
        assertNotNull("MIGRATION_3_4 should not be null", MIGRATION_3_4)
        assertNotNull("MIGRATION_4_5 should not be null", MIGRATION_4_5)
        assertNotNull("MIGRATION_5_6 should not be null", MIGRATION_5_6)
        assertNotNull("MIGRATION_6_7 should not be null", MIGRATION_6_7)
    }

    @Test
    fun allMigrationsArrayContainsAllMigrations() {
        // Test that ALL_MIGRATIONS contains all 6 migration objects
        assertEquals("ALL_MIGRATIONS should contain 6 migrations", 6, ALL_MIGRATIONS.size)

        // Verify each migration has correct start and end versions
        assertEquals("MIGRATION_1_2 should go from version 1 to 2", 1, ALL_MIGRATIONS[0].startVersion)
        assertEquals("MIGRATION_1_2 should go from version 1 to 2", 2, ALL_MIGRATIONS[0].endVersion)

        assertEquals("MIGRATION_2_3 should go from version 2 to 3", 2, ALL_MIGRATIONS[1].startVersion)
        assertEquals("MIGRATION_2_3 should go from version 2 to 3", 3, ALL_MIGRATIONS[1].endVersion)

        assertEquals("MIGRATION_3_4 should go from version 3 to 4", 3, ALL_MIGRATIONS[2].startVersion)
        assertEquals("MIGRATION_3_4 should go from version 3 to 4", 4, ALL_MIGRATIONS[2].endVersion)

        assertEquals("MIGRATION_4_5 should go from version 4 to 5", 4, ALL_MIGRATIONS[3].startVersion)
        assertEquals("MIGRATION_4_5 should go from version 4 to 5", 5, ALL_MIGRATIONS[3].endVersion)

        assertEquals("MIGRATION_5_6 should go from version 5 to 6", 5, ALL_MIGRATIONS[4].startVersion)
        assertEquals("MIGRATION_5_6 should go from version 5 to 6", 6, ALL_MIGRATIONS[4].endVersion)

        assertEquals("MIGRATION_6_7 should go from version 6 to 7", 6, ALL_MIGRATIONS[5].startVersion)
        assertEquals("MIGRATION_6_7 should go from version 6 to 7", 7, ALL_MIGRATIONS[5].endVersion)
    }

    @Test
    fun migrationsHaveUniqueVersionRanges() {
        // Ensure no overlapping version ranges
        val versionRanges = ALL_MIGRATIONS.map { it.startVersion to it.endVersion }

        // Check that each migration's end version matches the next migration's start version
        for (i in 0 until versionRanges.size - 1) {
            val currentEnd = versionRanges[i].second
            val nextStart = versionRanges[i + 1].first
            assertEquals("Migration $i end version should match migration ${i + 1} start version",
                currentEnd, nextStart)
        }
    }

    @Test
    fun migrationsCoverCompleteRange() {
        // Ensure migrations cover the complete range from version 1 to 7
        val firstMigration = ALL_MIGRATIONS.first()
        val lastMigration = ALL_MIGRATIONS.last()

        assertEquals("First migration should start at version 1", 1, firstMigration.startVersion)
        assertEquals("Last migration should end at version 7", 7, lastMigration.endVersion)
    }
}
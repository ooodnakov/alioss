package com.example.alias.data.pack

class CoverImageException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

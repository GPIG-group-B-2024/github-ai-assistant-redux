package uk.ac.york.gpig.teamb.aiassistant.database.exceptions

/** Represents an error during a database operation (i.e. at the repository level) */
class DatabaseOperationException(
    message: String?,
) : Exception(message)

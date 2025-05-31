package uk.ac.york.gpig.teamb.aiassistant.vcs.facades.git

sealed class PullRequestCreationException(
    message: String?,
) : Exception(message)

class FileNotFoundException(
    message: String?,
) : PullRequestCreationException(message)

class FileAlreadyExistsException(
    message: String?,
) : PullRequestCreationException(message)

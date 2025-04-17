package uk.ac.york.gpig.teamb.aiassistant.llm.client

sealed class OpenAIAPIException(
    message: String?,
) : Exception(message)

class PromptRefusedException(
    message: String?,
) : OpenAIAPIException(message)

class PromptTooLongException(
    message: String?,
) : OpenAIAPIException(message)

class MalformedOutputException(
    message: String?,
) : OpenAIAPIException(message)

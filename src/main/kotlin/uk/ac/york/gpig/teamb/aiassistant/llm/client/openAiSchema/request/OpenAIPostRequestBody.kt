package uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.request

import com.fasterxml.jackson.annotation.JsonProperty

data class OpenAIPostRequestBody(
    val model: String,
    @JsonProperty("response_format")
    val responseFormat: OpenAIResponseFormatField,
    val messages: List<OpenAIMessage>,
)

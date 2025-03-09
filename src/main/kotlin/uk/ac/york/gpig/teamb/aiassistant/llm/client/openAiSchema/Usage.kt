package uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Usage statistics for a [OpenAIResponseFormat]. See docs for the parent class for a list of omitted fields.
 * */
data class Usage(
    @JsonProperty("prompt_tokens")
    val promptTokens: Int,
    @JsonProperty("completion_tokens")
    val completionTokens: Int,
    @JsonProperty("total_tokens")
    val totalTokens: Int,
)

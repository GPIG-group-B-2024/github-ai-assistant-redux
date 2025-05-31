package uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A message passed to an OpenAI model. Can have several roles:
 * - `user` - message sent by the user
 * - `system` - system prompt (defines some common patterns in the model's behaviour)
 * - `assistant` - model's response
 */
data class OpenAIMessage(
    val role: Role,
    val content: String,
) {
    /** What kind of prompt this message is: system vs user. */
    enum class Role {
        @JsonProperty("user")
        USER,

        @JsonProperty("system")
        SYSTEM,

        @JsonProperty("assistant")
        ASSISTANT,
    }
}

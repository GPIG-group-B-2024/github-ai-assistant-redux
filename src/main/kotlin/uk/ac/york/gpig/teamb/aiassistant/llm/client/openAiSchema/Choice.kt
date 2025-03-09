package uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema

import com.fasterxml.jackson.annotation.JsonProperty
import uk.ac.york.gpig.teamb.aiassistant.llm.client.OpenAIMessage

/**
 * Represents a chat completion choice.
 *
 * We can assume we will only get one per request.
 * */
data class Choice(
    val index: Int,
    @JsonProperty("finish_reason")
    val finishReason: FinishReason,
    val message: Message,
) {
    data class Message(
        val role: OpenAIMessage.Role,
        val content: String?,
        /** if the message is refused (i.e. finishReason is [FinishReason.CONTENT_FILTER]), this will contain the reason why. otherwise it's null */
        val refusal: String?,
    )
}

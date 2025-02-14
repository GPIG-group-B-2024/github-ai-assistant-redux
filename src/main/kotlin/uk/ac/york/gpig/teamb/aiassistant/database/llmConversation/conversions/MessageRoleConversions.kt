package uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.conversions

import uk.ac.york.gpig.teamb.aiassistant.enums.LlmMessageRole
import uk.ac.york.gpig.teamb.aiassistant.llm.client.OpenAIMessage.Role

fun LlmMessageRole.toOpenAIMessageRole(): Role =
    when (this) {
        LlmMessageRole.SYSTEM -> Role.SYSTEM
        LlmMessageRole.USER -> Role.USER
        LlmMessageRole.ASSISTANT -> Role.ASSISTANT
    }

fun Role.toJooqMessageRole(): LlmMessageRole =
    when (this) {
        Role.USER -> LlmMessageRole.USER
        Role.SYSTEM -> LlmMessageRole.SYSTEM
        Role.ASSISTANT -> LlmMessageRole.ASSISTANT
    }

package uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.entities

import org.jooq.Record
import uk.ac.york.gpig.teamb.aiassistant.enums.LlmMessageRole
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_MESSAGE
import java.time.OffsetDateTime
import java.util.UUID

data class LLMMessageEntity(
    val id: UUID,
    val role: LlmMessageRole,
    val contents: String,
    val createdAt: OffsetDateTime,
) {
    companion object {
        fun fromJooq(record: Record) =
            LLMMessageEntity(
                id = record.get(LLM_MESSAGE.ID)!!,
                role = record.get(LLM_MESSAGE.ROLE)!!,
                contents = record.get(LLM_MESSAGE.CONTENT)!!,
                createdAt = record.get(LLM_MESSAGE.CREATED_AT)!!,
            )
    }
}

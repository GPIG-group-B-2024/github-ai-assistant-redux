package uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.entities

import org.jooq.Record
import uk.ac.york.gpig.teamb.aiassistant.enums.ConversationStatus
import uk.ac.york.gpig.teamb.aiassistant.tables.references.GITHUB_REPOSITORY
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_CONVERSATION
import java.time.OffsetDateTime
import java.util.UUID

data class LLMConversationEntity(
    val id: UUID,
    val repoId: UUID,
    val repoName: String,
    val issueId: Int,
    val createdAt: OffsetDateTime,
    val status: ConversationStatus,
) {
    companion object {
        fun fromJooq(record: Record) =
            LLMConversationEntity(
                id = record.get(LLM_CONVERSATION.ID)!!,
                repoId = record.get(LLM_CONVERSATION.REPO_ID)!!,
                repoName = record.get(GITHUB_REPOSITORY.FULL_NAME)!!,
                issueId = record.get(LLM_CONVERSATION.ISSUE_ID)!!,
                createdAt = record.get(LLM_CONVERSATION.CREATED_AT)!!,
                status = record.get(LLM_CONVERSATION.STATUS)!!,
            )
    }
}

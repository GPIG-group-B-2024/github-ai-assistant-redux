package uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.facades

import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import uk.ac.york.gpig.teamb.aiassistant.database.exceptions.DatabaseOperationException
import uk.ac.york.gpig.teamb.aiassistant.enums.ConversationStatus
import uk.ac.york.gpig.teamb.aiassistant.enums.LlmMessageRole
import uk.ac.york.gpig.teamb.aiassistant.tables.references.CONVERSATION_MESSAGE
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_CONVERSATION
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_MESSAGE
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class LLMConversationWriteFacade(
    @Autowired val ctx: DSLContext,
) {
    /** Link an **existing** message to an **existing** conversation */
    fun linkMessageToConversation(
        conversationId: UUID,
        messageId: UUID,
    ) = ctx
        .insertInto(CONVERSATION_MESSAGE)
        .columns(CONVERSATION_MESSAGE.CONVERSATION_ID, CONVERSATION_MESSAGE.MESSAGE_ID)
        .values(conversationId, messageId)
        .execute()
        .let { insertCount ->
            if (insertCount != 1) {
                throw DatabaseOperationException(
                    "Failed to link message $messageId to conversation $conversationId",
                )
            }
        }

    /** Store a single message (without linking it to a conversation) */
    fun storeMessage(
        id: UUID,
        role: LlmMessageRole,
        content: String,
    ) = ctx
        .insertInto(LLM_MESSAGE)
        .columns(LLM_MESSAGE.ID, LLM_MESSAGE.ROLE, LLM_MESSAGE.CONTENT, LLM_MESSAGE.CREATED_AT)
        .values(id, role, content, OffsetDateTime.now())
        .execute()
        .let { insertCount ->
            if (insertCount != 1) throw DatabaseOperationException("Failed to store message $id")
        }

    fun updateStatus(
        id: UUID,
        newStatus: ConversationStatus,
    ) = ctx
        .update(LLM_CONVERSATION)
        .set(LLM_CONVERSATION.STATUS, newStatus)
        .where(LLM_CONVERSATION.ID.eq(id))
        .execute()
        .let { updateCount ->
            if (updateCount != 1) {
                throw DatabaseOperationException("Failed to update status of conversation $id")
            }
        }

    /**
     * Create and store a new (empty) conversation, setting the status to
     * [ConversationStatus.IN_PROGRESS]
     */
    fun initConversation(
        id: UUID,
        repoId: UUID,
        issueId: Int,
    ) = ctx
        .insertInto(LLM_CONVERSATION)
        .columns(
            LLM_CONVERSATION.ID,
            LLM_CONVERSATION.REPO_ID,
            LLM_CONVERSATION.ISSUE_ID,
            LLM_CONVERSATION.CREATED_AT,
            LLM_CONVERSATION.STATUS,
        ).values(id, repoId, issueId, OffsetDateTime.now(), ConversationStatus.IN_PROGRESS)
        .execute()
        .let { insertCount ->
            if (insertCount != 1) {
                throw DatabaseOperationException("Failed to store conversation $id")
            }
        }
}

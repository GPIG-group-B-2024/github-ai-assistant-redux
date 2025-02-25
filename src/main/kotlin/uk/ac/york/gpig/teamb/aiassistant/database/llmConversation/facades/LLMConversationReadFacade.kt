package uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.facades

import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.entities.LLMConversationEntity
import uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.entities.LLMMessageEntity
import uk.ac.york.gpig.teamb.aiassistant.tables.references.CONVERSATION_MESSAGE
import uk.ac.york.gpig.teamb.aiassistant.tables.references.GITHUB_REPOSITORY
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_CONVERSATION
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_MESSAGE
import java.util.UUID

@Repository
class LLMConversationReadFacade(
    @Autowired private val ctx: DSLContext,
) {
    /**
     * List all messages in a given conversation in chronological order
     * */
    fun listConversationMessages(conversationId: UUID): List<LLMMessageEntity> =
        ctx.select(
            LLM_MESSAGE.ID,
            LLM_MESSAGE.CONTENT,
            LLM_MESSAGE.CREATED_AT,
            LLM_MESSAGE.ROLE,
        )
            .from(LLM_MESSAGE)
            .leftJoin(CONVERSATION_MESSAGE)
            .on(LLM_MESSAGE.ID.eq(CONVERSATION_MESSAGE.MESSAGE_ID))
            .where(CONVERSATION_MESSAGE.CONVERSATION_ID.eq(conversationId))
            .orderBy(LLM_MESSAGE.CREATED_AT)
            .fetch(LLMMessageEntity::fromJooq)

    /**
     * Get a conversation associated with a given issue in a given repository.
     * NOTE: assumes we only have one conversation per issue.
     * */
    fun fetchConversation(
        repoId: UUID,
        issueId: Int,
    ): LLMConversationEntity? =
        ctx.selectFrom(
            LLM_CONVERSATION
                .leftJoin(GITHUB_REPOSITORY)
                .on(LLM_CONVERSATION.REPO_ID.eq(GITHUB_REPOSITORY.ID)),
        )
            .where(
                LLM_CONVERSATION.REPO_ID.eq(
                    repoId,
                )
                    .and(
                        LLM_CONVERSATION.ISSUE_ID.eq(issueId),
                    ),
            )
            .fetchOne(LLMConversationEntity::fromJooq)

    fun fetchConversations() =
        ctx.selectFrom(
            LLM_CONVERSATION
                .leftJoin(GITHUB_REPOSITORY)
                .on(LLM_CONVERSATION.REPO_ID.eq(GITHUB_REPOSITORY.ID)),
        ).fetch(LLMConversationEntity::fromJooq)

    /**
     * Check if we already have a conversation for this issue in this repository.
     *
     * NOTE: this will probably need to be refactored if we plan to have multiple conversations per issue.
     * Will have to discuss implementations in a team meeting if this is the case.
     * */
    fun checkConversationExists(
        repoId: UUID,
        issueId: Int,
    ) = ctx.fetchExists(LLM_CONVERSATION.where(LLM_CONVERSATION.REPO_ID.eq(repoId).and(LLM_CONVERSATION.ISSUE_ID.eq(issueId))))
}

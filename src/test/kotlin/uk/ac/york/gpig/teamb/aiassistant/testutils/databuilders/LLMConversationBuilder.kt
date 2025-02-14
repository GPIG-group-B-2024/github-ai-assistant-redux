package uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders

import org.jooq.DSLContext
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_CONVERSATION
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.random.Random

class LLMConversationBuilder : TestDataWithIdBuilder<LLMConversationBuilder, UUID?>() {
    override var id: UUID? = UUID.randomUUID()
    var repoId: UUID = UUID.randomUUID()
    var issueId: Int = Random.nextInt(1000)
    var createdAt: OffsetDateTime = OffsetDateTime.now()

    var repoBuilder: GitRepoBuilder.() -> Unit = {
        this.id = this@LLMConversationBuilder.repoId
    }

    fun gitRepo(setup: GitRepoBuilder.() -> Unit): LLMConversationBuilder {
        repoBuilder = setup
        return this
    }

    companion object {
        @TestDSL
        fun conversation(setup: LLMConversationBuilder.() -> Unit) = LLMConversationBuilder().apply(setup)
    }

    override fun create(ctx: DSLContext): LLMConversationBuilder =
        this.create(ctx, LLM_CONVERSATION, LLM_CONVERSATION.ID) {
            val repoId = GitRepoBuilder.gitRepo(true, repoBuilder).create(ctx).id
            ctx.insertInto(LLM_CONVERSATION)
                .columns(
                    LLM_CONVERSATION.ID,
                    LLM_CONVERSATION.REPO_ID,
                    LLM_CONVERSATION.ISSUE_ID,
                    LLM_CONVERSATION.CREATED_AT,
                )
                .values(id, repoId, issueId, createdAt)
                .execute()
        }
}

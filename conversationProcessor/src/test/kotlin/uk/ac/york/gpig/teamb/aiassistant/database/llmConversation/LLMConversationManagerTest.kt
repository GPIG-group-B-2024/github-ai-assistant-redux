package uk.ac.york.gpig.teamb.aiassistant.database.llmConversation

import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.startsWith
import uk.ac.york.gpig.teamb.aiassistant.database.exceptions.NotFoundException
import uk.ac.york.gpig.teamb.aiassistant.enums.LlmMessageRole
import uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.request.OpenAIMessage.Role
import uk.ac.york.gpig.teamb.aiassistant.tables.references.CONVERSATION_MESSAGE
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_CONVERSATION
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_MESSAGE
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.testutils.assertions.isAfter
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.GitRepoBuilder.Companion.gitRepo
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.LLMConversationBuilder.Companion.conversation
import java.time.OffsetDateTime
import java.util.UUID

@AiAssistantTest
class LLMConversationManagerTest {
    @Autowired private lateinit var sut: LLMConversationManager

    @Autowired private lateinit var ctx: DSLContext

    @Test
    fun `creates a new conversation with a single message`() {
        val repoId = UUID.randomUUID()
        gitRepo { this.id = repoId }.create(ctx)

        val beforeCreationTimestamp = OffsetDateTime.now()
        val messageContent = "You are a software engineer. Engineer software!"
        sut.initConversationWithFirstMessage(repoId, 10, Role.SYSTEM, messageContent)

        val conversationResults = ctx.selectFrom(LLM_CONVERSATION).fetch()
        val messageResults = ctx.selectFrom(LLM_MESSAGE).fetch()

        expectThat(conversationResults)
            .hasSize(1)
            .get { this[0] }
            .and {
                get { this.repoId }.isEqualTo(repoId)
                get { this.issueId }.isEqualTo(10)
                get { this.createdAt }.isAfter(beforeCreationTimestamp)
            }

        expectThat(messageResults)
            .hasSize(1)
            .get { this[0] }
            .and {
                get { this.role }.isEqualTo(LlmMessageRole.SYSTEM)
                get { this.content }.isEqualTo(messageContent)
            }

        // at this point, we know that the message and conversation were created successfully.
        // check they were correctly linked with each other

        val messageId = messageResults.first().id
        val conversationId = conversationResults.first().id

        val messageConvoResults = ctx.selectFrom(CONVERSATION_MESSAGE).fetch()
        expectThat(messageConvoResults)
            .hasSize(1)
            .get { this[0] }
            .and {
                get { this.messageId }.isEqualTo(messageId)
                get { this.conversationId }.isEqualTo(conversationId)
            }
    }

    @Test
    fun `throws for unknown repo`() {
        gitRepo { this.id = UUID.randomUUID() }.create(ctx)

        expectThrows<NotFoundException.NotFoundByIdException> {
            sut.initConversationWithFirstMessage(UUID.randomUUID(), 10, Role.SYSTEM, "Some message")
        }.and { get { this.message }.isNotNull().startsWith("Could not find Git repo") }
    }

    @Test
    fun `throws when conversation already exists`() {
        val repoId = UUID.randomUUID()
        val issueId = 10
        conversation {
            this.issueId = issueId
            this.gitRepo { this.id = repoId }
        }.create(ctx)

        expectThrows<IllegalStateException> {
            sut.initConversationWithFirstMessage(repoId, issueId, Role.SYSTEM, "Some message")
        }.and {
            get { this.message }
                .isEqualTo("Conversation about issue $issueId in repo $repoId already exists")
        }
    }
}

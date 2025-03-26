package uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.facades

import org.jooq.DSLContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import uk.ac.york.gpig.teamb.aiassistant.enums.LlmMessageRole
import uk.ac.york.gpig.teamb.aiassistant.tables.references.CONVERSATION_MESSAGE
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_CONVERSATION
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_MESSAGE
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.testutils.assertions.isAfter
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.GitRepoBuilder.Companion.gitRepo
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.LLMConversationBuilder.Companion.conversation
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.LLMMessageBuilder.Companion.message
import java.time.OffsetDateTime
import java.util.UUID

@AiAssistantTest
class LLMConversationWriteFacadeTest {
    @Autowired
    private lateinit var sut: LLMConversationWriteFacade

    @Autowired
    private lateinit var ctx: DSLContext

    @Nested
    inner class InitConversationTest {
        @Test
        fun `stores conversation and links it to github issue`() {
            val conversationId = UUID.randomUUID()
            val repoId = UUID.randomUUID()
            gitRepo {
                this.id = repoId
            }.create(ctx)
            val issueId = 10
            val beforeInsertTimestamp = OffsetDateTime.now()
            Thread.sleep(500)

            sut.initConversation(conversationId, repoId, issueId)

            val result = ctx.selectFrom(LLM_CONVERSATION).fetch()

            expectThat(result).hasSize(1).get { this[0] }.and {
                get { this.id }.isEqualTo(conversationId)
                get { this.repoId }.isEqualTo(repoId)
                get { this.issueId }.isEqualTo(issueId)
                get { this.createdAt }.isAfter(beforeInsertTimestamp)
            }
        }
    }

    @Nested
    inner class StoreMessageTest {
        @Test
        fun `stores a single message`() {
            val messageId = UUID.randomUUID()
            val messageContents = "This is my first message!"
            val beforeCreationTimestamp = OffsetDateTime.now()
            Thread.sleep(500)

            sut.storeMessage(messageId, LlmMessageRole.USER, messageContents)

            val result = ctx.selectFrom(LLM_MESSAGE).fetch()
            expectThat(result).hasSize(1).get { this[0] }.and {
                get { this.id }.isEqualTo(messageId)
                get { this.role }.isEqualTo(LlmMessageRole.USER)
                get { this.content }.isEqualTo(messageContents)
                get { this.createdAt }.isAfter(beforeCreationTimestamp)
            }
        }
    }

    @Nested
    @DisplayName("tests for linking a message to a conversation")
    inner class LinkMessageToConversationTest {
        @Test
        fun `links a message to an existing conversation`() {
            val messageId = UUID.randomUUID()
            val conversationId = UUID.randomUUID()
            conversation {
                this.id = conversationId
            }.create(ctx)
            message {
                this.id = messageId
            }.create(ctx)

            sut.linkMessageToConversation(conversationId, messageId)

            val result = ctx.selectFrom(CONVERSATION_MESSAGE).fetch()

            expectThat(result).hasSize(1).get { this[0] }.and {
                get { this.messageId }.isEqualTo(messageId)
                get { this.conversationId }.isEqualTo(conversationId)
            }
        }
    }
}

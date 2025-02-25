package uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.facades

import org.jooq.DSLContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import strikt.api.expectDoesNotThrow
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEmpty
import strikt.assertions.isNull
import strikt.assertions.isTrue
import strikt.assertions.withNotNull
import uk.ac.york.gpig.teamb.aiassistant.enums.LlmMessageRole
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.LLMConversationBuilder.Companion.conversation
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.LLMConversationMessageBuilder.Companion.conversationWithMessages
import java.util.UUID
import kotlin.random.Random

@AiAssistantTest
class LLMConversationReadFacadeTest {
    @Autowired
    private lateinit var sut: LLMConversationReadFacade

    @Autowired
    private lateinit var ctx: DSLContext

    @Nested
    @DisplayName("Retrieving a list of messages in a conversation")
    inner class ListMessagesTest {
        @Test
        fun `lists messages in chronological order (earliest first)`() {
            val conversationId = UUID.randomUUID()
            conversationWithMessages {
                this.conversation {
                    this.id = conversationId
                }
                this.message {
                    this.role = LlmMessageRole.USER
                    this.content = "This is my first message!"
                }
                this.message {
                    this.role = LlmMessageRole.USER
                    this.content = "This is my second one!"
                }
                this.message {
                    this.role = LlmMessageRole.USER
                    this.content = "This is the third!"
                }
            }.create(ctx)

            val result = sut.listConversationMessages(conversationId)

            expectThat(result).isNotEmpty().get { this.map { it.role to it.contents } }.containsExactly(
                LlmMessageRole.USER to "This is my first message!",
                LlmMessageRole.USER to "This is my second one!",
                LlmMessageRole.USER to "This is the third!",
            )
        }

        @Test
        fun `handles empty conversation`() {
            val conversationId = UUID.randomUUID()
            conversation {
                this.id = conversationId
            }.create(ctx)

            expectDoesNotThrow { sut.listConversationMessages(conversationId) }.isEmpty()
        }
    }

    @Nested
    @DisplayName("checkConversationExists test")
    inner class CheckConversationExistsTest {
        @Test
        fun `reports true for existing conversation`() {
            val conversationId = UUID.randomUUID()
            val issueId = 10
            val repoId = UUID.randomUUID()
            conversation {
                this.id = conversationId
                this.issueId = issueId
                this.gitRepo {
                    this.id = repoId
                }
            }.create(ctx)
            val result = sut.checkConversationExists(repoId, issueId)
            expectThat(result).isTrue()
        }

        @Test
        fun `reports false for non-existing conversation`() {
            val conversationId = UUID.randomUUID()
            val repoId = UUID.randomUUID()
            conversation {
                this.id = conversationId
                this.issueId = 10
                this.gitRepo {
                    this.id = repoId
                }
            }.create(ctx)
            val result = sut.checkConversationExists(UUID.randomUUID(), Random.nextInt(100))
            expectThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("fetchConversation test")
    inner class FetchConversationTest {
        @Test
        fun `fetches existing conversation`() {
            val conversationId = UUID.randomUUID()
            val issueId = 10
            val repoId = UUID.randomUUID()
            val repoName = "my-fancy-repo"
            conversation {
                this.id = conversationId
                this.issueId = issueId
                this.gitRepo {
                    this.id = repoId
                    this.fullName = repoName
                }
            }.create(ctx)

            val result = sut.fetchConversation(repoId, issueId)

            expectThat(result).withNotNull {
                get { this.id }.isEqualTo(conversationId)
                get { this.issueId }.isEqualTo(issueId)
                get { this.repoId }.isEqualTo(repoId)
                get { this.repoName }.isEqualTo(repoName)
            }
        }

        @Test
        fun `returns null when conversation not found`() {
            val conversationId = UUID.randomUUID()
            val repoId = UUID.randomUUID()
            conversation {
                this.id = conversationId
                this.issueId = 10
                this.gitRepo {
                    this.id = repoId
                }
            }.create(ctx)

            val result = sut.fetchConversation(UUID.randomUUID(), 50)

            expectThat(result).isNull()
        }
    }
}

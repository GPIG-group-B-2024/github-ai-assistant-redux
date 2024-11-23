package uk.ac.york.gpig.teamb.aiassistant.controllers

import com.google.gson.Gson
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import strikt.api.expectThrows
import uk.ac.york.gpig.teamb.aiassistant.managers.IssueManager
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload

@SpringBootTest
class WebhookControllerTest {
    @MockkBean
    private lateinit var issueManager: IssueManager

    @Autowired
    private lateinit var sut: WebhookController

    @Test
    fun `passes issues event payload to issue manager`() {
        // setup
        every { issueManager.processNewIssue(any()) } just runs
        val issueBody =
            WebhookPayload(
                action = WebhookPayload.Action.OPENED,
                issue =
                    WebhookPayload.Issue(
                        id = 12345L,
                        title = "Important issue title",
                        body = "Important issue body",
                        number = 5,
                    ),
                repository =
                    WebhookPayload.Repository(
                        "my-test-repository",
                        "my-test-url",
                    ),
                comment =
                    WebhookPayload.Comment(
                        id = 1L,
                        user = WebhookPayload.Comment.User(""),
                        body = "",
                    ),
            )
        // act
        sut.receiveNewWebhook("issues", Gson().toJson(issueBody))
        // verify
        verify {
            issueManager.processNewIssue(issueBody)
        }
    }

    @Test
    fun `ignores issues events with action other than OPENED`() {
        // setup
        every { issueManager.processNewIssue(any()) } just runs
        val issueBody =
            WebhookPayload(
                action = WebhookPayload.Action.CLOSED,
                issue =
                    WebhookPayload.Issue(
                        id = 12345L,
                        title = "Important issue title",
                        body = "Important issue body",
                        number = 5,
                    ),
                repository =
                    WebhookPayload.Repository(
                        "my-test-repository",
                        "my-test-url",
                    ),
                comment =
                    WebhookPayload.Comment(
                        id = 1L,
                        user = WebhookPayload.Comment.User(""),
                        body = "",
                    ),
            )
        // act
        sut.receiveNewWebhook("issues", Gson().toJson(issueBody)) // to string function to be able to put enum here rather than string?
        // verify
        verify(exactly = 0) {
            issueManager.processNewIssue(any())
        }
    }

    @Test
    fun `passes issue_comment event payload to issue manager`() {
        // setup
        every { issueManager.processNewIssueComment(any()) } just runs
        val issueBody =
            WebhookPayload(
                action = WebhookPayload.Action.CREATED,
                issue =
                    WebhookPayload.Issue(
                        id = 12345L,
                        title = "Important issue title",
                        body = "Important issue body",
                        number = 5,
                    ),
                repository =
                    WebhookPayload.Repository(
                        "my-test-repository",
                        "my-test-url",
                    ),
                comment =
                    WebhookPayload.Comment(
                        id = 274L,
                        user = WebhookPayload.Comment.User("pangreor"),
                        body = "this is a comment",
                    ),
            )
        // act
        sut.receiveNewWebhook("issue_comment", Gson().toJson(issueBody))
        // verify
        verify {
            issueManager.processNewIssueComment(issueBody)
        }
    }

    @Test
    fun `ignores issue_comment events with action other than CREATED`() {
        // setup
        every { issueManager.processNewIssueComment(any()) } just runs
        val issueBody =
            WebhookPayload(
                action = WebhookPayload.Action.CLOSED,
                issue =
                    WebhookPayload.Issue(
                        id = 12345L,
                        title = "Important issue title",
                        body = "Important issue body",
                        number = 5,
                    ),
                repository =
                    WebhookPayload.Repository(
                        "my-test-repository",
                        "my-test-url",
                    ),
                comment =
                    WebhookPayload.Comment(
                        id = 274L,
                        user = WebhookPayload.Comment.User("pangreor"),
                        body = "this is a comment",
                    ),
            )
        // act
        sut.receiveNewWebhook(
            "issue_comment",
            Gson().toJson(issueBody),
        ) // to string function to be able to put enum here rather than string?
        // verify
        verify(exactly = 0) {
            issueManager.processNewIssueComment(any())
        }
    }

    @Test
    fun `ignores unhandled event types`() {
        // setup
        every { issueManager.processNewIssue(any()) } just runs
        every { issueManager.processNewIssueComment(any()) } just runs
        val issueBody =
            WebhookPayload(
                action = WebhookPayload.Action.CLOSED,
                issue =
                    WebhookPayload.Issue(
                        id = 12345L,
                        title = "Important issue title",
                        body = "Important issue body",
                        number = 5,
                    ),
                repository =
                    WebhookPayload.Repository(
                        "my-test-repository",
                        "my-test-url",
                    ),
                comment =
                    WebhookPayload.Comment(
                        id = 274L,
                        user = WebhookPayload.Comment.User("pangreor"),
                        body = "this is a comment",
                    ),
            )
        // act
        // verify
        expectThrows<IllegalArgumentException> {
            sut.receiveNewWebhook("foo", Gson().toJson(issueBody))
        }
    }
}

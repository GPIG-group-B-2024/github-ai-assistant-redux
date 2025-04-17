package uk.ac.york.gpig.teamb.aiassistant.controllers

import com.google.gson.Gson
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import strikt.api.expectThrows
import uk.ac.york.gpig.teamb.aiassistant.llm.LLMManager
import uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas.LLMPullRequestData
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload
import uk.ac.york.gpig.teamb.aiassistant.vcs.VCSManager

@AiAssistantTest
class WebhookControllerTest {
    @MockkBean private lateinit var vcsManager: VCSManager

    @MockkBean(relaxed = true)
    private lateinit var llmManager: LLMManager

    @Autowired private lateinit var sut: WebhookController

    @Test
    fun `passes issues event payload to issue manager`() {
        // setup
        val pullRequestData =
            LLMPullRequestData(
                pullRequestBody = "This is a pull request description",
                pullRequestTitle = "This is a pull request title",
                updatedFiles =
                    listOf(
                        LLMPullRequestData.Change(
                            type = LLMPullRequestData.ChangeType.CREATE,
                            filePath = "path/to/a/file.txt",
                            newContents = "This is some cool text",
                        ),
                        LLMPullRequestData.Change(
                            type = LLMPullRequestData.ChangeType.CREATE,
                            filePath = "path/to/a/differentFile.txt",
                            newContents = "This text is boring",
                        ),
                    ),
            )
        every { vcsManager.processChanges(any(), any(), any()) } just runs
        every { llmManager.produceIssueSolution(any(), any()) } returns (pullRequestData)
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
                repository = WebhookPayload.Repository("my-test-repository", "my-test-url"),
                comment = WebhookPayload.Comment(id = 1L, user = WebhookPayload.Comment.User(""), body = ""),
            )
        // act
        sut.receiveNewWebhook("issues", Gson().toJson(issueBody))
        // verify
        verify {
            llmManager.produceIssueSolution(issueBody.repository.fullName, issueBody.issue)
            vcsManager.processChanges(issueBody.repository, issueBody.issue, pullRequestData)
        }
    }

    @Test
    fun `ignores issues events with action other than OPENED`() {
        // setup
        val pullRequestData =
            LLMPullRequestData(
                pullRequestBody = "This is a pull request description",
                pullRequestTitle = "This is a pull request title",
                updatedFiles =
                    listOf(
                        LLMPullRequestData.Change(
                            type = LLMPullRequestData.ChangeType.CREATE,
                            filePath = "path/to/a/file.txt",
                            newContents = "This is some cool text",
                        ),
                        LLMPullRequestData.Change(
                            type = LLMPullRequestData.ChangeType.CREATE,
                            filePath = "path/to/a/differentFile.txt",
                            newContents = "This text is boring",
                        ),
                    ),
            )
        every { vcsManager.processChanges(any(), any(), any()) } just runs
        every { llmManager.produceIssueSolution(any(), any()) } returns (pullRequestData)
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
                repository = WebhookPayload.Repository("my-test-repository", "my-test-url"),
                comment = WebhookPayload.Comment(id = 1L, user = WebhookPayload.Comment.User(""), body = ""),
            )
        // act
        sut.receiveNewWebhook(
            "issues",
            Gson().toJson(issueBody),
        ) // to string function to be able to put enum here rather than string?
        // verify
        verify(exactly = 0) {
            llmManager.produceIssueSolution(issueBody.repository.fullName, issueBody.issue)
            vcsManager.processChanges(issueBody.repository, issueBody.issue, pullRequestData)
        }
    }

    @Test
    fun `passes issue_comment event payload to issue manager`() {
        // setup
        every { vcsManager.processNewIssueComment(any()) } just runs
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
                repository = WebhookPayload.Repository("my-test-repository", "my-test-url"),
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
        verify { vcsManager.processNewIssueComment(issueBody) }
    }

    @Test
    fun `ignores issue_comment events with action other than CREATED`() {
        // setup
        every { vcsManager.processNewIssueComment(any()) } just runs
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
                repository = WebhookPayload.Repository("my-test-repository", "my-test-url"),
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
        verify(exactly = 0) { vcsManager.processNewIssueComment(any()) }
    }

    @Test
    fun `ignores unhandled event types`() {
        // setup
        every { vcsManager.processChanges(any(), any(), any()) } just runs
        every { vcsManager.processNewIssueComment(any()) } just runs
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
                repository = WebhookPayload.Repository("my-test-repository", "my-test-url"),
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

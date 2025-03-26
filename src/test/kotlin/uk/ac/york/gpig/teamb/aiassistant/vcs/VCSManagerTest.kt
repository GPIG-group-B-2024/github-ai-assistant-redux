package uk.ac.york.gpig.teamb.aiassistant.vcs

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas.LLMPullRequestData
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload
import uk.ac.york.gpig.teamb.aiassistant.vcs.facades.git.GitFacade
import uk.ac.york.gpig.teamb.aiassistant.vcs.facades.github.GitHubFacade

@AiAssistantTest
class VCSManagerTest {
    @Autowired
    private lateinit var sut: VCSManager

    @MockkBean(relaxed = true)
    private lateinit var gitFacade: GitFacade

    @MockkBean(relaxed = true)
    private lateinit var gitHubFacade: GitHubFacade

    @Test
    fun `creates pull request from newly created feature branch when a new issue is opened`() {
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
                        fullName = "my-test-repository",
                        url = "my-test-url",
                    ),
                comment =
                    WebhookPayload.Comment(
                        id = 1L,
                        user = WebhookPayload.Comment.User(""),
                        body = "",
                    ),
            )

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

        sut.processChanges(issueBody.repository, issueBody.issue, pullRequestData)
        val expectedBranchName = "5-important-issue-title"

        verify {
            gitFacade.createBranch(any(), expectedBranchName)
        }

        verify {
            gitHubFacade.createPullRequest(
                repoName = "my-test-repository",
                baseBranch = "main",
                featureBranch = expectedBranchName,
                title = "This is a pull request title",
                body = "This is a pull request description",
            )
        }
    }

    @Test
    fun `creates and uses a fresh installation token to access github when proccessing a new issue`() {
        every { gitHubFacade.generateInstallationToken() } returns "my-fancy-token"

        val repository =
            WebhookPayload.Repository(
                fullName = "my-test-repository",
                url = "my-test-url",
            )

        val issue =
            WebhookPayload.Issue(
                id = 12345L,
                title = "Important issue title",
                body = "Important issue body",
                number = 5,
            )

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

        sut.processChanges(repository, issue, pullRequestData)
        verify(exactly = 1) { gitHubFacade.generateInstallationToken() }
        verify { gitFacade.pushBranch(any(), any(), "my-fancy-token") }
    }

    @Test
    fun `writes comment to an issue when a new issue comment is created`() {
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
                        fullName = "my-test-repository",
                        url = "my-test-url",
                    ),
                comment =
                    WebhookPayload.Comment(
                        id = 1L,
                        user = WebhookPayload.Comment.User("pangreor"),
                        body = "what a comment!",
                    ),
            )

        sut.processNewIssueComment(issueBody)

        verify {
            gitHubFacade.createComment(
                repoName = "my-test-repository",
                issueNumber = 5,
                body = "This is a helpful comment",
            )
        }
    }

    @Test
    fun `does not write a comment when newest comment is written by itself`() {
        every { gitHubFacade.createComment(any(), any(), any()) } just runs
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
                        fullName = "my-test-repository",
                        url = "my-test-url",
                    ),
                comment =
                    WebhookPayload.Comment(
                        id = 1L,
                        user = WebhookPayload.Comment.User("gpig-ai-assistant[bot]"),
                        body = "what a comment!",
                    ),
            )
        // Act
        sut.processNewIssueComment(issueBody)

        // Verify
        verify(exactly = 0) {
            gitHubFacade.createComment(any(), any(), any())
        }
    }
}

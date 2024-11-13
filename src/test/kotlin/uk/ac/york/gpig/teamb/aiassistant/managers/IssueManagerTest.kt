package uk.ac.york.gpig.teamb.aiassistant.managers

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import uk.ac.york.gpig.teamb.aiassistant.facades.git.GitFacade
import uk.ac.york.gpig.teamb.aiassistant.facades.github.GitHubFacade
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload
import kotlin.test.Test

@SpringBootTest
class IssueManagerTest {
    @Autowired
    private lateinit var sut: IssueManager

    @MockkBean(relaxed = true)
    private lateinit var gitFacade: GitFacade

    @MockkBean(relaxed = true)
    private lateinit var gitHubFacade: GitHubFacade

    @Test
    fun `creates pull request from newly created feature branch`() {
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
            )

        sut.processNewIssue(issueBody)
        val expectedBranchName = "5-important-issue-title"

        verify {
            gitFacade.createBranch(any(), expectedBranchName)
        }

        verify {
            gitHubFacade.createPullRequest(
                repoName = "my-test-repository",
                baseBranch = "main",
                featureBranch = expectedBranchName,
                title = "Important issue title",
                body = match { it.contains("closes") && it.contains("Important issue body") },
            )
        }
    }

    @Test
    fun `creates and uses a fresh installation token to access github`() {
        every { gitHubFacade.generateInstallationToken() } returns "my-fancy-token"
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
            )

        sut.processNewIssue(issueBody)
        verify(exactly = 1) { gitHubFacade.generateInstallationToken() }
        verify { gitFacade.pushBranch(any(), any(), "my-fancy-token") }
    }
}

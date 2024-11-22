package uk.ac.york.gpig.teamb.aiassistant.controllers

import com.google.gson.Gson
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload
import uk.ac.york.gpig.teamb.aiassistant.vcs.IssueManager

@AiAssistantTest
class IssueCreationControllerTest {
    @MockkBean
    private lateinit var issueManager: IssueManager

    @Autowired
    private lateinit var sut: IssueCreationController

    @Test
    fun `passes issue event payload to issue manager`() {
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
            )
        // act
        sut.receiveNewIssue(Gson().toJson(issueBody))
        // verify
        verify {
            issueManager.processNewIssue(issueBody)
        }
    }

    @Test
    fun `ignores issue events with action other than OPENED`() {
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
            )
        // act
        sut.receiveNewIssue(Gson().toJson(issueBody))
        // verify
        verify(exactly = 0) {
            issueManager.processNewIssue(any())
        }
    }
}

package uk.ac.york.gpig.teamb.aiassistant.mockMvc

import com.google.gson.Gson
import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import uk.ac.york.gpig.teamb.aiassistant.controllers.WebhookController
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload
import uk.ac.york.gpig.teamb.aiassistant.vcs.IssueManager

@WebMvcTest(controllers = [WebhookController::class])
class MockMvcWebhookPayload {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var issueManager: IssueManager

    @Test
    fun `test_invalid_header`()  {
        val mockWebhook =
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
        mockMvc.perform(
            post(
                "/new-issue",
            ).header(
                "x-github-event",
                "issues",
            ).header("x-github-hook-installation-target-type", "repository").contentType(MediaType.APPLICATION_JSON).content(
                Gson().toJson(mockWebhook),
            ),
        )

        verify {
            issueManager.processNewIssue(mockWebhook)
        }
    }
}

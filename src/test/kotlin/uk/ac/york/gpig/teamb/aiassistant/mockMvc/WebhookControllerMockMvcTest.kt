package uk.ac.york.gpig.teamb.aiassistant.mockMvc

import com.google.gson.Gson
import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.ac.york.gpig.teamb.aiassistant.controllers.WebhookController
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload
import uk.ac.york.gpig.teamb.aiassistant.vcs.IssueManager

@WebMvcTest(controllers = [WebhookController::class])
class WebhookControllerMockMvcTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var issueManager: IssueManager

    @Test
    fun `receiving opened issue`() {
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

    @Test
    fun `receiving new comment`() {
        val mockWebhook =
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
                "issue_comment",
            ).header("x-github-hook-installation-target-type", "repository").contentType(MediaType.APPLICATION_JSON).content(
                Gson().toJson(mockWebhook),
            ),
        )

        verify {
            issueManager.processNewIssueComment(mockWebhook)
        }
    }

    @Test
    fun `should log unsupported action for valid event type and not call issueManager`() {
        val mockWebhook =
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

        verify(exactly = 0) {
            issueManager.processNewIssue(mockWebhook)
        }
    }

    @Test
    fun `missing x-github-event header and return bad request`() {
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
            ).header("x-github-hook-installation-target-type", "repository").contentType(MediaType.APPLICATION_JSON).content(
                Gson().toJson(mockWebhook),
            ),
        )
            .andExpect(status().isBadRequest)  // Expect 400 bad request

//        verify {
//            issueManager.processNewIssue(mockWebhook)
//        }
    }




}

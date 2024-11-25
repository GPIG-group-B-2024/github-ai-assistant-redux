package uk.ac.york.gpig.teamb.aiassistant.controllers

import com.google.gson.Gson
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.ac.york.gpig.teamb.aiassistant.managers.IssueManager
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload
import java.sql.DriverManager.println


@WebMvcTest(IssueCreationController::class)
class IssueCreationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var issueManager: IssueManager


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
                        number = 5
                    ),
                repository =
                    WebhookPayload.Repository(
                        "my-test-repository",
                        "my-test-url"
                    ),
            )

        val requestBody = Gson().toJson(issueBody)
        // act
        val result = mockMvc.perform(
            post("/new-issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)

        //println("Response: ${result.andReturn().response.contentAsString}") // Debugging

        // verify
        verify(exactly = 1) {
            issueManager.processNewIssue(withArg { actual ->
                assertEquals(issueBody.action, actual.action)
                assertEquals(issueBody.issue.id, actual.issue.id)
                assertEquals(issueBody.repository.fullName, actual.repository.fullName)
            })
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

        val requestBody = Gson().toJson(issueBody)

        // act
        mockMvc.perform(
            post("/new-issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)

        // verify
        verify(exactly = 0) {
            issueManager.processNewIssue(any())
        }
    }
}

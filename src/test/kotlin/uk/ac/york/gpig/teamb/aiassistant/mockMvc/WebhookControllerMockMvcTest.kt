package uk.ac.york.gpig.teamb.aiassistant.mockMvc

import com.google.gson.Gson
import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload.Action
import uk.ac.york.gpig.teamb.aiassistant.utils.web.WebhookValidationFilter.Companion.ALGORITHM
import uk.ac.york.gpig.teamb.aiassistant.vcs.VCSManager
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@AiAssistantTest
@AutoConfigureMockMvc
class WebhookControllerMockMvcTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var vcsManager: VCSManager

    val mockWebhook =
        WebhookPayload(
            action = Action.OPENED,
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

    @OptIn(ExperimentalStdlibApi::class)
    private fun createMockSignature(
        mockPayload: WebhookPayload,
        mockSecret: String = "my-fancy-secret",
    ): String {
        // Just following the process outlined in github docs here: Use the (mock) secret as the key and encode the payload
        val hmacSha256 = Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(mockSecret.toByteArray(), ALGORITHM)) }
        val requestBody = Gson().toJson(mockPayload).toByteArray()
        // add the prefix and attach the hex string in the header
        return "sha256=${hmacSha256.doFinal(requestBody).toHexString()}"
    }

    @Test
    fun `receiving opened issue`() {
        val mockSignature = createMockSignature(mockWebhook)
        mockMvc.perform(
            post(
                "/webhooks",
            ).header(
                "x-github-event",
                "issues",
            ).header("x-github-hook-installation-target-type", "integration")
                .header("x-hub-signature-256", mockSignature)
                .contentType(MediaType.APPLICATION_JSON).content(
                    Gson().toJson(mockWebhook),
                ),
        ).andExpect {
            status().isOk
        }

        verify {
            vcsManager.processNewIssue(mockWebhook)
        }
    }

    @Test
    fun `receiving new comment`() {
        val commentCreatedMockWebhook = mockWebhook.copy(action = Action.CREATED)
        val mockSignature = createMockSignature(commentCreatedMockWebhook)
        mockMvc.perform(
            post(
                "/webhooks",
            ).header(
                "x-github-event",
                "issue_comment",
            )
                .header("x-github-hook-installation-target-type", "integration")
                .header("x-hub-signature-256", mockSignature)
                .contentType(MediaType.APPLICATION_JSON).content(
                    Gson().toJson(commentCreatedMockWebhook),
                ),
        ).andExpect {
            status().isOk
        }

        verify {
            vcsManager.processNewIssueComment(commentCreatedMockWebhook)
        }
    }

    @Test
    fun `should log unsupported action for valid event type and not call issueManager`() {
        val unsupportedOpWebhook = mockWebhook.copy(action = Action.CLOSED)
        val mockSignature = createMockSignature(unsupportedOpWebhook)
        mockMvc.perform(
            post(
                "/webhooks",
            ).header(
                "x-github-event",
                "issues",
            )
                .header("x-github-hook-installation-target-type", "repository")
                .header("x-hub-signature-256", mockSignature)
                .contentType(MediaType.APPLICATION_JSON).content(
                    Gson().toJson(unsupportedOpWebhook),
                ),
        )

        verify(exactly = 0) {
            vcsManager.processNewIssue(unsupportedOpWebhook)
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["im not the right value!"])
    fun `should block request when x-github-installation-target-type is missing or wrong`(targetTypeHeader: String?) {
        val mockSignature = createMockSignature(mockWebhook)
        mockMvc.perform(
            post(
                "/webhooks",
            ).header(
                "x-github-event",
                "issues",
            ).apply {
                header("x-hub-signature-256", mockSignature)
                if (targetTypeHeader != null) {
                    header("x-github-installation-target-type", targetTypeHeader)
                }
            }.contentType(MediaType.APPLICATION_JSON).content(
                Gson().toJson(mockWebhook),
            ),
        ).andExpect {
            status().isForbidden
        }

        verify(exactly = 0) {
            vcsManager.processNewIssue(mockWebhook)
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["im not the right value!"])
    fun `should block request when x-hub-signature is missing or wrong`(signatureHeader: String?) {
        mockMvc.perform(
            post(
                "/webhooks",
            ).header(
                "x-github-event",
                "issues",
            ).apply {
                header("x-github-installation-target-type", "repository")
                if (signatureHeader != null) {
                    header("x-hub-signature-256", signatureHeader)
                }
            }.contentType(MediaType.APPLICATION_JSON).content(
                Gson().toJson(mockWebhook),
            ),
        ).andExpect {
            if (signatureHeader == null) status().isUnauthorized else status().isForbidden
        }

        verify(exactly = 0) {
            vcsManager.processNewIssue(mockWebhook)
        }
    }

    @Test
    fun `missing x-github-event header and return bad request`() {
        val mockSignature = createMockSignature(mockWebhook)
        mockMvc.perform(
            post(
                "/webhooks",
            )
                .header("x-github-hook-installation-target-type", "integration")
                .header("x-hub-signature-256", mockSignature)
                .contentType(MediaType.APPLICATION_JSON).content(
                    Gson().toJson(mockWebhook),
                ),
        )
            .andExpect(status().isBadRequest) // Expect 400 bad request
    }
}

package uk.ac.york.gpig.teamb.aiassistant.mockMvc

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.ac.york.gpig.teamb.aiassistant.controllers.ConversationAdminController
import uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.LLMConversationManager
import uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.entities.LLMConversationEntity
import java.time.Instant

@WebMvcTest(ConversationAdminController::class)
class ConversationAdminControllerMockMvcTest {
    @MockkBean(relaxed = true)
    private lateinit var llmConversationManager: LLMConversationManager

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `allows viewing the dashboard when user is registered at the uni of york`() {
        every { llmConversationManager.fetchConversations() } returns emptyList<LLMConversationEntity>()
        val token =
            OidcIdToken(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                mapOf(
                    "sub" to "12345",
                    "email" to "my-user@york.ac.uk",
                ),
            )
        val user = DefaultOidcUser(emptyList<SimpleGrantedAuthority>(), token)
        mockMvc.perform(
            get("/admin/conversations")
                .with(oidcLogin().oidcUser(user)),
        ).andExpect(
            status().isOk,
        )
        verify {
            llmConversationManager.fetchConversations()
        }
    }

    @Test
    fun `blocks users from non-uni addresses`() {
        every { llmConversationManager.fetchConversations() } returns emptyList<LLMConversationEntity>()
        val token =
            OidcIdToken(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                mapOf(
                    "sub" to "12345",
                    "email" to "my-user@funky-domain.com",
                ),
            )
        val user = DefaultOidcUser(emptyList<SimpleGrantedAuthority>(), token)
        mockMvc.perform(
            get("/admin/conversations")
                .with(oidcLogin().oidcUser(user)),
        ).andExpect(
            status().isForbidden,
        )
        verify(exactly = 0) {
            llmConversationManager.fetchConversations()
        }
    }
}

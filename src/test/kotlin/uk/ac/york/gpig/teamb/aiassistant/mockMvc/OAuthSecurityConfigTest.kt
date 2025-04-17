package uk.ac.york.gpig.teamb.aiassistant.mockMvc

import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.ac.york.gpig.teamb.aiassistant.database.c4.C4Manager
import uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.LLMConversationManager
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "oauth-test")
class OAuthSecurityConfigTest {
    @Autowired private lateinit var mockMvc: MockMvc

    @Suppress("UNUSED")
    @MockkBean(relaxed = true)
    private lateinit var llmConversationManager: LLMConversationManager

    @Suppress("UNUSED")
    @MockkBean(relaxed = true)
    private lateinit var clientRegistrationRepository: ClientRegistrationRepository

    @Suppress("UNUSED")
    @MockkBean(relaxed = true)
    private lateinit var c4Manager: C4Manager

    @Test
    fun `should return 401 when accessing admin without authentication`() {
        mockMvc.perform(get("/admin")).andExpect(status().isUnauthorized)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/actuator", "/actuator/health"])
    fun `should allow GET request to actuator endpoints with no auth`(endpoint: String) {
        mockMvc.perform(get(endpoint)).andExpect(status().isOk)
    }

    @Test
    fun `should allow GET requests for stylesheets with no auth`() {
        mockMvc.perform(get("/css/main.css")).andExpect(status().isOk)
    }

    @Test
    fun `should reject (403) requests to unknown endpoints when authenticated`() {
        val token =
            OidcIdToken(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                mapOf("sub" to "12345", "email" to "my-user@funky-domain.co.uk"),
            )
        val user = DefaultOidcUser(emptyList<SimpleGrantedAuthority>(), token)
        mockMvc
            .perform(get("/funky-url").with(oidcLogin().oidcUser(user)))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should reject (401) requests to unknown endpoints with no auth`() {
        mockMvc.perform(get("/funky-url")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should allow access to admin with correct authority`() {
        val token =
            OidcIdToken(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                mapOf("sub" to "12345", "email" to "my-user@york.ac.uk"),
            )
        val user = DefaultOidcUser(listOf(SimpleGrantedAuthority("dashboard:view")), token)
        mockMvc
            .perform(get("/admin/conversations").with(oidcLogin().oidcUser(user)))
            .andExpect(status().isOk)
    }

    @Test
    fun `returns 403 when accessing admin dashboard with wrong permissions`() {
        val token =
            OidcIdToken(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                mapOf("sub" to "12345", "email" to "my-user@funky-domain.co.uk"),
            )
        val user = DefaultOidcUser(emptyList<SimpleGrantedAuthority>(), token)
        mockMvc
            .perform(get("/admin/conversations").with(oidcLogin().oidcUser(user)))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `returns 403 when accessing structurizr write endpoint with guest permissions`() {
        val token =
            OidcIdToken(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                mapOf("sub" to "12345", "email" to "my-user@funky-domain.co.uk"),
            )
        val user = DefaultOidcUser(listOf(SimpleGrantedAuthority("dashboard:view")), token)
        mockMvc
            .perform(post("/admin/structurizr").with(csrf()).with(oidcLogin().oidcUser(user)))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `returns 200 when accessing structurizr write endpoint with admin permissions`() {
        val token =
            OidcIdToken(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                mapOf("sub" to "12345", "email" to "my-user@funky-domain.co.uk"),
            )
        val user =
            DefaultOidcUser(
                listOf(
                    SimpleGrantedAuthority("structurizr:write"),
                    SimpleGrantedAuthority("dashboard:view"),
                ),
                token,
            )
        mockMvc
            .perform(
                post("/admin/structurizr")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                        "repo_url": "my-fancy-repo",
                        "raw_structurizr": "workspace \"my-workspace\"{}"
                        }
                        """.trimIndent(),
                    ).with(csrf())
                    .with(oidcLogin().oidcUser(user)),
            ).andExpect(status().isOk)
    }
}

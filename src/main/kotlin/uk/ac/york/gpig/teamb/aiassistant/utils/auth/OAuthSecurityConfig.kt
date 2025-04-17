package uk.ac.york.gpig.teamb.aiassistant.utils.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Configuration
@EnableWebSecurity
class OAuthSecurityConfig(
    private val authorityMapper: DashboardAuthorityMapper,
    @Value("\${okta.oauth2.issuer}") private val issuer: String,
    @Value("\${okta.oauth2.client-id}") private val clientId: String,
) {
    /**
     * Set up security so that:
     * - Users with the right permissions have read-only access to the dashboard
     * - No authentication is required for the webhook endpoint (handled in [WebhookValidationFilter])
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize("/css/**", permitAll) // make sure stylesheets are not blocked
                authorize(HttpMethod.GET, "/actuator/**", permitAll)
                authorize(HttpMethod.POST, "/webhooks", permitAll)
                authorize("/error/**", permitAll)
                // dashboard URL's
                authorize("/admin/structurizr", hasAuthority("structurizr:write"))
                authorize(HttpMethod.GET, "/", hasAuthority("dashboard:view"))
                authorize("/admin", hasAuthority("dashboard:view"))
                authorize("/admin/**", hasAuthority("dashboard:view"))
                authorize(HttpMethod.GET, "/error/**", permitAll) // let users see the pretty error page
                // this is a standard practice, reject all requests to unknown URL's
                authorize(anyRequest, denyAll)
            }
            csrf {
                ignoringRequestMatchers(
                    "/webhooks",
                ) // we will authenticate this separately by using the github secret
            }
            exceptionHandling { accessDeniedPage = "/error/403" }
            oauth2Login { userInfoEndpoint { oidcUserService = authorityMapper } }
            oauth2ResourceServer { jwt {} }
            logout { addLogoutHandler(logoutHandler) }
        }
        return http.build()
    }

    private val logoutHandler =
        LogoutHandler { req, resp, auth ->
            val baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
            resp.sendRedirect("${issuer}v2/logout?client_id=$clientId&returnTo=$baseUrl")
        }
}

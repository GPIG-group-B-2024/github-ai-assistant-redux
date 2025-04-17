package uk.ac.york.gpig.teamb.aiassistant.utils.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

@Component
class DashboardAuthorityMapper(
    @Value("\${app_settings.auth0_groups-claim}") private val groupsClaim: String,
) : OidcUserService() {
    /**
     * Assign app roles based on the auth0 user data.
     *
     * The auth0 configuration automatically grants access to all users with a York email. To access
     * the dashboard, the "guest" role is enough
     */
    override fun loadUser(userRequest: OidcUserRequest?): OidcUser? {
        val oidcUser: OAuth2User = super.loadUser(userRequest)
        val oldAuthorities = oidcUser.authorities

        @Suppress("UNCHECKED_CAST") // not ideal, but we know the format of this is consistent
        val assignedRoles =
            (oidcUser.attributes[groupsClaim] as List<String>).map(::SimpleGrantedAuthority)
        val additionalPermissions = mutableSetOf<SimpleGrantedAuthority>()
        val hasReadOnlyRole =
            assignedRoles.any { it.authority == "ROLE_ADMIN" || it.authority == "ROLE_GUEST" }
        val isAdmin = assignedRoles.any { it.authority == "ROLE_ADMIN" }
        when {
            isAdmin ->
                additionalPermissions.addAll(
                    listOf(
                        SimpleGrantedAuthority("dashboard:view"),
                        SimpleGrantedAuthority("structurizr:write"),
                    ),
                )
            hasReadOnlyRole -> additionalPermissions.add(SimpleGrantedAuthority("dashboard:view"))
        }

        return DefaultOidcUser(additionalPermissions + oldAuthorities, userRequest!!.idToken)
    }
}

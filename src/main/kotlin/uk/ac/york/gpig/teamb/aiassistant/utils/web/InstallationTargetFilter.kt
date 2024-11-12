package uk.ac.york.gpig.teamb.aiassistant.utils.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * GitHub seems to send duplicate webhook payloads which can only be differentiated by their header.
 * For this project, we are only interested in "repository" level events. No-op for others.
 *
 * */
@Component
class InstallationTargetFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val targetHeader = request.getHeader("x-github-hook-installation-target-type")
        when (targetHeader) {
            "repository" -> filterChain.doFilter(request, response)
            else -> {}
        }
    }
}

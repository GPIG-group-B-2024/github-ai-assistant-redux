package uk.ac.york.gpig.teamb.aiassistant.controllers

import com.structurizr.dsl.StructurizrDslParserException
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import uk.ac.york.gpig.teamb.aiassistant.database.exceptions.NotFoundException
import kotlin.reflect.jvm.jvmName

@ControllerAdvice(
    assignableTypes = [ConversationAdminController::class, StructurizrNotationController::class],
)
class DashboardExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException,
        @AuthenticationPrincipal principal: OidcUser?,
        model: Model,
        resp: HttpServletResponse,
    ): String =
        when (ex.statusCode) {
            // populate the "unauthorized" template with user data
            HttpStatus.FORBIDDEN -> model.addAttribute("profile", principal?.claims).let { "error/403" }
            // some other error, return the generic error page
            else -> model.addAttribute("message", ex.message).let { "error" }
        }.also {
            resp.status = ex.statusCode.value()
        } // make sure response is not 200 when rendering a template

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(
        ex: NotFoundException,
        model: Model,
        resp: HttpServletResponse,
    ): String =
        "error"
            .also {
                model.addAttribute("error", ex::class.simpleName ?: ex::class.jvmName)
                model.addAttribute("message", ex.message)
                resp.status = HttpStatus.NOT_FOUND.value()
            }

    @ExceptionHandler(StructurizrDslParserException::class)
    fun handleStructurizrException(
        ex: StructurizrDslParserException,
        model: Model,
        resp: HttpServletResponse,
    ): String =
        "error"
            .also {
                model.addAttribute("error", ex::class.simpleName ?: ex::class.jvmName)
                model.addAttribute("message", ex.message)
                resp.status = HttpStatus.BAD_REQUEST.value()
            }
}

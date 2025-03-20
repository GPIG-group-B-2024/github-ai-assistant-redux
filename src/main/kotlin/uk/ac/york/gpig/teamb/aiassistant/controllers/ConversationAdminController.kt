package uk.ac.york.gpig.teamb.aiassistant.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ResponseStatusException
import uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.LLMConversationManager
import java.util.UUID

@Controller
@RequestMapping("/admin")
class ConversationAdminController(
    @Autowired
    private val llmConversationManager: LLMConversationManager,
) {
    @GetMapping("/conversations")
    fun index(
        model: Model,
        @AuthenticationPrincipal principal: OidcUser,
    ): String {
        if (!principal.email.endsWith("@york.ac.uk")) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        val conversations = llmConversationManager.fetchConversations()
        model.run {
            addAttribute("conversationCount", conversations.size)
            addAttribute("data", conversations)
            addAttribute("profile", principal.claims)
        }
        return "admin/index"
    }

    @GetMapping
    fun redirectToIndex() = "redirect:/admin/conversations"

    @GetMapping("/conversations/{conversationId}")
    fun conversationPage(
        model: Model,
        @PathVariable conversationId: UUID,
        @AuthenticationPrincipal principal: OidcUser,
    ): String {
        if (!principal.email.endsWith("@york.ac.uk")) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        val messages = llmConversationManager.fetchConversationMessages(conversationId)
        model.run {
            addAttribute("conversationId", conversationId)
            addAttribute("messageCount", messages.size)
            addAttribute("data", messages)
            addAttribute("profile", principal.claims)
        }
        return "admin/conversation"
    }
}

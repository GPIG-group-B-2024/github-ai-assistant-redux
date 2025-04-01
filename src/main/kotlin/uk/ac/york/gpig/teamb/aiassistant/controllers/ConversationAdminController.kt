package uk.ac.york.gpig.teamb.aiassistant.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.LLMConversationManager
import java.util.UUID

@Controller
@RequestMapping("")
class ConversationAdminController(
    @Autowired
    private val llmConversationManager: LLMConversationManager,
) {
    @GetMapping("/admin/conversations")
    fun index(
        model: Model,
        @AuthenticationPrincipal principal: OidcUser,
    ): String {
        val conversations = llmConversationManager.fetchConversations()
        model.run {
            addAttribute("conversationCount", conversations.size)
            addAttribute("data", conversations)
            addAttribute("profile", principal.claims)
        }
        return "admin/index"
    }

    @GetMapping
    fun redirectFromRoot() = "redirect:/admin/conversations"

    @GetMapping("/admin")
    fun redirectFromAdminRoot() = "redirect:/admin/conversations"

    @GetMapping("/admin/conversations/{conversationId}")
    fun conversationPage(
        model: Model,
        @PathVariable conversationId: UUID,
        @AuthenticationPrincipal principal: OidcUser,
    ): String {
        val messages = llmConversationManager.fetchConversationMessages(conversationId)
        val conversation = llmConversationManager.fetchConversation(conversationId)
        model.run {
            addAttribute("conversationId", conversationId)
            addAttribute("messageCount", messages.size)
            addAttribute("data", messages)
            addAttribute("conversation", conversation)
            addAttribute("profile", principal.claims)
        }
        return "admin/conversation"
    }
}

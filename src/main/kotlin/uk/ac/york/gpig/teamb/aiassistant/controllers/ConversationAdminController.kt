package uk.ac.york.gpig.teamb.aiassistant.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.LLMConversationManager
import java.util.UUID

@Controller
@RequestMapping("/admin")
class ConversationAdminController(
    @Autowired
    private val llmConversationManager: LLMConversationManager,
) {
    @GetMapping("/conversations")
    fun index(model: Model): String {
        val conversations = llmConversationManager.fetchConversations()
        model.run {
            addAttribute("conversationCount", conversations.size)
            addAttribute("data", conversations)
        }
        return "admin/index"
    }

    @GetMapping
    fun redirectToIndex() = "redirect:/admin/conversations"

    @GetMapping("/conversations/{conversationId}")
    fun conversationPage(
        model: Model,
        @PathVariable conversationId: UUID,
    ): String {
        val messages = llmConversationManager.fetchConversationMessages(conversationId)
        model.run {
            addAttribute("conversationId", conversationId)
            addAttribute("messageCount", messages.size)
            addAttribute("data", messages)
        }
        return "admin/conversation"
    }
}

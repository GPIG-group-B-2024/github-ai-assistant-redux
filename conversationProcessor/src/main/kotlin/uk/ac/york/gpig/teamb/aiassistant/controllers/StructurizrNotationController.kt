package uk.ac.york.gpig.teamb.aiassistant.controllers

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import uk.ac.york.gpig.teamb.aiassistant.controllers.data.StructurizrWorkspaceData
import uk.ac.york.gpig.teamb.aiassistant.database.c4.C4Manager

@Controller
class StructurizrNotationController(
    val c4Manager: C4Manager,
) {
    /**
     * An endpoint for manually storing a structurizr diagram and associating it with a github repo.
     */
    @PostMapping("/admin/structurizr")
    fun writeStructurizrRepresentation(
        @Valid @ModelAttribute("workspaceData") workspaceData: StructurizrWorkspaceData,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        if (bindingResult.hasErrors()) {
            val principal = SecurityContextHolder.getContext().authentication.principal as OidcUser
            model.run {
                addAttribute("profile", principal.claims)
                addAttribute("workspaceData", workspaceData)
            }
            return "admin/structurizr/structurizr_input_form"
        }
        c4Manager.initializeWorkspace(
            workspaceData.repoName,
            workspaceData.repoUrl,
            workspaceData.rawStructurizr,
        )
        return "admin/structurizr/structurizr_success"
    }

    @GetMapping("/admin/structurizr")
    fun writeStructurizrForm(
        model: Model,
        @AuthenticationPrincipal principal: OidcUser,
    ): String {
        model.run {
            addAttribute("profile", principal.claims)
            addAttribute("workspaceData", StructurizrWorkspaceData())
        }
        return "admin/structurizr/structurizr_input_form"
    }
}

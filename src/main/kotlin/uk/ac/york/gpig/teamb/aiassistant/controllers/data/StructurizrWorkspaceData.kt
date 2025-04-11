package uk.ac.york.gpig.teamb.aiassistant.controllers.data

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/** Represents the data submitted through the form in the admin dashboard. */
data class StructurizrWorkspaceData(
    // repo name in the owner/repo format e.g. some-text-with-no-slashes/more-text-no-slashes
    @field:NotBlank
    @field:Pattern(
        "[^\\/]+\\/[^\\/]+",
        message = "Please provide the repository name in the owner/repo format",
    )
    val repoName: String = "",
    @field:NotBlank
    @field:Pattern(
        "https:\\/\\/github.com\\/[^\\/]+\\/[^\\/]+",
        message = "Please provide the repository url in the https://github.com/owner/repo format",
    )
    val repoUrl: String = "",
    @field:NotBlank val rawStructurizr: String = "",
)

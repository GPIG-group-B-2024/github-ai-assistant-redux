package uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas

data class LLMPullRequestData(
    val pullRequestBody: String,
    val pullRequestTitle: String,
    val updatedFiles: List<UpdatedFile>,
) {
    data class UpdatedFile(
        val fullName: String,
        val newContents: String,
    )
}

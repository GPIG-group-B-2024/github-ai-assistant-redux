package uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas

data class LLMPullRequestData(
    val pullRequestBody: String,
    val pullRequestTitle: String,
    val updatedFiles: List<Change>,
) {
    data class Change(
        // TODO: make enum
        val type: String,
        val filePath: String,
        val newContents: String,
    )
}

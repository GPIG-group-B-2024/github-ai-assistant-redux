package uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas

data class LLMPullRequestData(
    val pullRequestBody: String,
    val pullRequestTitle: String,
    val updatedFiles: List<Change>,
) {
    data class Change(
        val type: ChangeType,
        val filePath: String,
        val newContents: String,
    )

    /**
     * The change type of a model suggested change

     * */
    enum class ChangeType {
        MODIFY,

        CREATE,

        DELETE,
    }
}

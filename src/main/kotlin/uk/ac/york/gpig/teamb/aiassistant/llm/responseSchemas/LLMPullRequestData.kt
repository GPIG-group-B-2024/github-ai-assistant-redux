package uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas

import com.google.gson.annotations.SerializedName

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
        @SerializedName("modify")
        MODIFY,

        @SerializedName("create")
        CREATE,

        @SerializedName("delete")
        DELETE,
    }
}

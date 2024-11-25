package uk.ac.york.gpig.teamb.aiassistant.utils.types

import com.google.gson.annotations.SerializedName

/**
 * Describes (partially) a Github webhook payload.
 *
 * For details on payload schema, see [webhook API docs](https://docs.github.com/en/webhooks/webhook-events-and-payloads#issues)
 *
 * */
data class WebhookPayload(
    val issue: Issue,
    val action: Action,
    val repository: Repository,
    val comment: Comment,
) {
    data class Issue(
        val title: String,
        val body: String,
        val number: Int,
        val id: Long,
    )

    data class Comment(
        val id: Long,
        val user: User,
        val body: String,
    ) {
        data class User(val login: String)
    }

    /**
     * The action being done to an issue (has many more options, must be added here as needed)
     *
     * */
    enum class Action {
        @SerializedName("opened")
        OPENED,

        @SerializedName("closed")
        CLOSED,

        @SerializedName("created")
        CREATED,
    }

    /**
     * The repository associated with the webhook
     * */
    data class Repository(
        /**
         * The name of the url in the `Owner/repo` format
         * */
        @SerializedName("full_name")
        val fullName: String,
        /**
         * The url with which the repo can be accessed. Must be appended with `.git`
         * */
        @SerializedName("html_url")
        val url: String,
    )
}

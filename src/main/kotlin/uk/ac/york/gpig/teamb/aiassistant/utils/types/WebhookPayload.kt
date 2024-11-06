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
) {
    data class Issue(
        val title: String,
        val body: String,
        val number: Int,
        val id: Long,
    )

    /**
     * The action being done to an issue (has many more options, must be added here as needed)
     *
     * */
    enum class Action {
        @SerializedName("opened")
        OPENED,

        @SerializedName("closed")
        CLOSED,
    }
}

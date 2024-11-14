package uk.ac.york.gpig.teamb.aiassistant.controllers

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import uk.ac.york.gpig.teamb.aiassistant.managers.IssueManager
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload

/**
 * Receives incoming webhook events.
 *
 * The exact event types are configured on GitHub, and currently only include updates to issues.
 * */
@RestController
class WebhookController(
   private val issueManager: IssueManager,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/new-issue")
    fun receiveNewWebhook(
        @RequestHeader("x-github-event") eventType: String,
        @RequestBody body: String,
    ) {
        val issueContents = Gson().fromJson(body, WebhookPayload::class.java)
        when (eventType to issueContents.action) {
            ("issues" to WebhookPayload.Action.OPENED) -> {
                logger.info("Received new open issue with id ${issueContents.issue.id}")
                issueManager.processNewIssue(issueContents)
            }
            ("issue_comment" to WebhookPayload.Action.CREATED) -> {
                logger.info("Received new comment on issue with id ${issueContents.issue.id}")
                issueManager.processNewIssueComment(issueContents)
            }
            else -> logger.info("No handler for event type $eventType")
        }
    }
}

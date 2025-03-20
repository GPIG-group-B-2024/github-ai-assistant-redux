package uk.ac.york.gpig.teamb.aiassistant.controllers

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import uk.ac.york.gpig.teamb.aiassistant.llm.LLMManager
import uk.ac.york.gpig.teamb.aiassistant.utils.types.EventType
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload
import uk.ac.york.gpig.teamb.aiassistant.vcs.VCSManager

/**
 * Receives incoming webhook events.
 *
 * The exact event types are configured on GitHub, and currently only include updates to issues.
 * */
@RestController
class WebhookController(
    private val vcsManager: VCSManager,
    private val llmManager: LLMManager,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/webhooks")
    fun receiveNewWebhook(
        @RequestHeader("x-github-event") eventType: String,
        @RequestBody body: String,
    ) {
        val issueContents = Gson().fromJson(body, WebhookPayload::class.java)
        when (EventType.fromString(eventType) to issueContents.action) {
            (EventType.ISSUES to WebhookPayload.Action.OPENED) -> {
                logger.info("Received new open issue with id ${issueContents.issue.id}")
                val (issue, _, repository, comment) = issueContents
                val pullRequestData = llmManager.produceIssueSolution(repository.fullName, issue)
                vcsManager.processChanges(repository, issue, pullRequestData)
            }
            (EventType.ISSUE_COMMENT to WebhookPayload.Action.CREATED) -> {
                logger.info("Received new comment on issue with id ${issueContents.issue.id}")
                vcsManager.processNewIssueComment(issueContents)
            }
            else -> logger.info("No handler for event type $eventType")
        }
    }
}

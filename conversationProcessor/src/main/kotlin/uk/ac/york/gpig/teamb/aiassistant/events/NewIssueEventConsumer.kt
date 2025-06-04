package uk.ac.york.gpig.teamb.aiassistant.events

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.MessageListener
import uk.ac.york.gpig.teamb.Action
import uk.ac.york.gpig.teamb.WebhookPayload
import uk.ac.york.gpig.teamb.aiassistant.llm.LLMManager
import uk.ac.york.gpig.teamb.events.Topics

typealias IssueId = Long

@KafkaListener(topics = [Topics.TOPIC_NEW_ISSUE])
class NewIssueEventConsumer(
    val llmManager: LLMManager,
    val producer: PullRequestPlanProducer,
) : MessageListener<IssueId, WebhookPayload> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun onMessage(data: ConsumerRecord<IssueId?, WebhookPayload>) {
        val payload = data.value()
        when (payload.action) {
            (Action.opened) -> {
                logger.info("Received new open issue with id ${payload.issue.id}")
                val (conversationId, pullRequestData) =
                    llmManager.produceIssueSolution(
                        payload.repository.fullName.toString(),
                        payload.issue,
                    )
                producer.sendMessage(conversationId, pullRequestData)
            }
            else -> logger.info("No handler for event type ${payload.action}")
        }
    }
}

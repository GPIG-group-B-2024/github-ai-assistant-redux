package uk.ac.york.gpig.teamb.aiassistant.events

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.MessageListener
import uk.ac.york.gpig.teamb.WebhookPayload
import uk.ac.york.gpig.teamb.events.Topics

@KafkaListener(topics = [Topics.TOPIC_NEW_ISSUE])
class NewIssueEventConsumer : MessageListener<Long, WebhookPayload> {
    override fun onMessage(data: ConsumerRecord<Long?, WebhookPayload?>) {
        TODO("Not yet implemented")
    }
}

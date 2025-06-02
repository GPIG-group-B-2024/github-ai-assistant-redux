package uk.ac.york.gpig.teamb.aiassistant.events

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.MessageListener
import uk.ac.york.gpig.teamb.events.Topics
import java.util.UUID

@KafkaListener(topics = [Topics.TOPIC_PR_STATUS_UPDATE])
class PullRequestEventConsumer : MessageListener<UUID, Any> {
    override fun onMessage(data: ConsumerRecord<UUID?, Any?>) {
        TODO("Not yet implemented")
    }
}

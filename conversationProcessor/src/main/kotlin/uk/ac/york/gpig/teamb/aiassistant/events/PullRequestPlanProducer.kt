package uk.ac.york.gpig.teamb.aiassistant.events

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import uk.ac.york.gpig.teamb.events.Topics
import java.util.UUID

@Service
class PullRequestPlanProducer(
    val template: KafkaTemplate<UUID, Any>,
) {
    fun sendMessage() = template.send(Topics.TOPIC_NEW_CODE_CHANGE, UUID.randomUUID(), "hello")
}

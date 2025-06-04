package uk.ac.york.gpig.teamb.codechangeprocessor.events

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import uk.ac.york.gpig.teamb.events.Topics
import java.util.UUID

@Service
class CodeChangeCompleteProducer(
    val template: KafkaTemplate<UUID, Any>,
) {
    fun sendMessage() = template.send(Topics.TOPIC_PR_STATUS_UPDATE, UUID.randomUUID(), "hello")
}

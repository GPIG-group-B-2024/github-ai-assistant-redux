package uk.ac.york.gpig.teamb.codechangeprocessor.events

import org.springframework.kafka.annotation.KafkaListener
import uk.ac.york.gpig.teamb.events.Topics

@KafkaListener(topics = [Topics.TOPIC_NEW_CODE_CHANGE])
class NewCodeChangeConsumer

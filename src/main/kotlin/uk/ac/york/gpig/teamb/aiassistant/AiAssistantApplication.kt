package uk.ac.york.gpig.teamb.aiassistant

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile

@SpringBootApplication
class AiAssistantApplication

fun main(args: Array<String>) {
    runApplication<AiAssistantApplication>(*args)
}

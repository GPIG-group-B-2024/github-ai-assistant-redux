package uk.ac.york.gpig.teamb.aiassistant.llm.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

/**
 * Performs web requests to the OpenAI API
 * */
@Service
class OpenAIClient {
    /**
     * We use a different endpoint address for mocking OpenAI requests in testing.
     * In production, use the normal OpenAI endpoint
     * */
    @Value("\${app_settings.openai_api_endpoint:https://api.openai.com/v1/chat/completions}")
    private lateinit var openAIEndpoint: String

    private val dummyToken = "my-secret-token" // TODO: get a token and actually use it

    fun performStructuredOutputQuery(requestData: OpenAIStructuredRequestData): String? {
        val client =
            RestClient.builder()
                .baseUrl(openAIEndpoint)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bearer $dummyToken") // attach the token to the request
                .build()

        val rawJsonOutput =
            client.post()
                .body(requestData)
                .retrieve()
                .body(String::class.java)
        return rawJsonOutput
    }
}

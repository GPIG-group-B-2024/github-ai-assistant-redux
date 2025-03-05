package uk.ac.york.gpig.teamb.aiassistant.llm.client

import com.google.gson.Gson
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

    @Value("\${app_settings.openai_api_key}")
    private lateinit var apiKey: String // TODO: setup token (we can use application-dev for now)

    /**
     * Send a single request to the OpenAI API and receive a structured output of a given type.
     *
     * @param requestData information about the model version, the previous messages in the conversation (if present), and the output format
     * @param outputClass the class into which the raw output will be parsed
     * */
    fun <TOutput : Any> performStructuredOutputQuery(
        requestData: OpenAIStructuredRequestData,
        outputClass: Class<TOutput>,
    ): TOutput =
        RestClient.builder()
            .baseUrl(openAIEndpoint)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Authorization", "Bearer $apiKey") // attach the token to the request
            .build()
            .run {
                post()
                    .body(requestData)
                    .retrieve()
                    .body(String::class.java)
            }
            .let { Gson().fromJson(it, outputClass) }
}

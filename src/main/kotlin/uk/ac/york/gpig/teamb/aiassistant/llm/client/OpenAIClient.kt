package uk.ac.york.gpig.teamb.aiassistant.llm.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.request.OpenAIStructuredRequestData
import uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.response.FinishReason
import uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.response.OpenAIResponseFormat

/**
 * Performs web requests to the OpenAI API
 * */
@Service
class OpenAIClient(
    val objectMapper: ObjectMapper,
) {
    /**
     * We use a different endpoint address for mocking OpenAI requests in testing.
     * In production, use the normal OpenAI endpoint
     * */
    @Value("\${app_settings.openai_api_endpoint:https://api.openai.com/v1/chat/completions/}")
    private lateinit var openAIEndpoint: String

    @Value("\${app_settings.openai_api_key}")
    private lateinit var apiKey: String // TODO: setup token (we can use application-dev for now)

    /**
     * Send a single request to the OpenAI API and receive a structured output of a given type.
     *
     * @param requestData information about the model version, the previous messages in the conversation (if present), and the output format
     *
     * @return ChatGPT response, conforming to the provided response format
     * */
    fun <TOutput : Any> performStructuredOutputQuery(requestData: OpenAIStructuredRequestData<TOutput>): TOutput =
        RestClient.builder()
            .baseUrl(openAIEndpoint)
            .build()
            .post()
            .headers {
                it.contentType = MediaType.APPLICATION_JSON
                it.setBearerAuth(apiKey)
            }
            .body(requestData.toPostRequestBody())
            .retrieve()
            .body(OpenAIResponseFormat::class.java)?.let { response ->
                val (_, finishReason, message) = response.choices.first() // index is always 0
                when (finishReason) {
                    FinishReason.LENGTH -> throw PromptTooLongException(
                        "Response ID '${response.id}' exceeded length limit (total usage: ${response.usage.totalTokens})",
                    )
                    FinishReason.CONTENT_FILTER -> throw PromptRefusedException(
                        "Response with ID '${response.id}' refused with reason: ${message.refusal}",
                    )
                    FinishReason.STOP -> objectMapper.readValue(message.content, requestData.responseFormatClass.java)
                }
            } ?: throw MalformedOutputException("OpenAI API responded with no body")
}

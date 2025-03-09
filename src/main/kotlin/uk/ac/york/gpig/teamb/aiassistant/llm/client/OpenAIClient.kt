package uk.ac.york.gpig.teamb.aiassistant.llm.client

import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.FinishReason
import uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.OpenAIResponseFormat
import uk.ac.york.gpig.teamb.aiassistant.utils.types.toJsonSchema

/**
 * Performs web requests to the OpenAI API
 * */
@Service
class OpenAIClient(
    val gson: Gson,
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
            .body(
                mapOf(
                    "model" to requestData.model,
                    "response_format" to requestData.responseFormatClass.toJsonSchema(),
                    "messages" to requestData.messages,
                ),
            )
            .retrieve()
            .body(OpenAIResponseFormat::class.java)?.let { response ->
                val (_, finishReason, message) = response.choices.first() // index is always 0
                when (finishReason) {
                    FinishReason.LENGTH -> throw PromptTooLongException(
                        "Request ID '${response.id}' exceeded length limit (total usage: ${response.usage.totalTokens})",
                    )
                    FinishReason.CONTENT_FILTER -> throw PromptRefusedException(
                        "Request with ID '${response.id}' refused with reason: ${message.refusal}",
                    )
                    FinishReason.STOP -> {
                        message.content?.let {
                            gson.fromJson(it, requestData.responseFormatClass.java)
                        } ?: throw NoContentException("Request with ID '${response.id}' returned a blank message")
                    }
                }
            } ?: throw MalformedOutputException("OpenAI API responded with no body")
}

package uk.ac.york.gpig.teamb.aiassistant.llm.client

import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

/**
 * Performs web requests to the OpenAI API
 * */
@Service
class OpenAIClient(
    @Autowired val gson: Gson
) {
    /**
     * We use a different endpoint address for mocking OpenAI requests in testing.
     * In production, use the normal OpenAI endpoint
     * */
    @Value("\${app_settings.openai_api_endpoint:https://api.openai.com/v1/chat/completions}")
    private lateinit var openAIEndpoint: String

    @Value("app_settings.openai_api_key:my-fancy-token")
    private lateinit var dummyToken: String // TODO: setup token (we can use application-dev for now)

    fun <TOutput : Any> performStructuredOutputQuery(
        requestData: OpenAIStructuredRequestData,
        expectedOutput: TOutput,
    ): TOutput =
        RestClient.builder()
            .baseUrl(openAIEndpoint)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Authorization", "Bearer $dummyToken") // attach the token to the request
            .build()
            .run {
                post()
                    .body(requestData)
                    .retrieve()
                    .body(String::class.java)
            }.let {
                gson.fromJson(it, expectedOutput::class.java)
            }
}

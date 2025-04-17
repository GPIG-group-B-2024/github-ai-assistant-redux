package uk.ac.york.gpig.teamb.aiassistant.llm.client

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import strikt.api.expectThrows
import strikt.assertions.contains
import strikt.assertions.isNotNull
import uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.request.OpenAIMessage
import uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.request.OpenAIStructuredRequestData
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest

@AiAssistantTest
@WireMockTest(httpPort = 3001)
class OpenAIClientTest {
    @Autowired private lateinit var sut: OpenAIClient

    @Test
    fun `smoke test`() {
        data class Car(
            val make: String,
            val model: String,
            val horsePower: Int,
        )
        stubFor(
            post("/v1/chat/completions/")
                .willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                        {
                          "id": "chatcmpl-123456",
                          "object": "chat.completion",
                          "created": 1728933352,
                          "model": "gpt-4o-2024-08-06",
                          "choices": [
                            {
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": "{\"make\":\"BMW\",\"model\":\"3-series\",\"horsepower\":150}",
                                "refusal": null
                              },
                              "logprobs": null,
                              "finish_reason": "stop"
                            }
                          ],
                          "usage": {
                            "prompt_tokens": 19,
                            "completion_tokens": 10,
                            "total_tokens": 29,
                            "prompt_tokens_details": {
                              "cached_tokens": 0
                            },
                            "completion_tokens_details": {
                              "reasoning_tokens": 0,
                              "accepted_prediction_tokens": 0,
                              "rejected_prediction_tokens": 0
                            }
                          },
                          "system_fingerprint": "fp_6b68a8204b"
                        }""",
                        ),
                ),
        )
        sut.performStructuredOutputQuery(
            OpenAIStructuredRequestData(
                model = "latest-chatgpt",
                messages =
                    listOf(
                        OpenAIMessage(
                            role = OpenAIMessage.Role.SYSTEM,
                            content = "You are a car inventor. When prompted, create a new car.",
                        ),
                        OpenAIMessage(
                            role = OpenAIMessage.Role.USER,
                            content =
                                """
                                Create a powerful car with a confidence-inspiring model name. 
                                Use a well-respected manufacturer as the make.
                                """.trimIndent(),
                        ),
                    ),
                responseFormatClass = Car::class,
            ),
        )
        verify(
            postRequestedFor(urlEqualTo("/v1/chat/completions/"))
                .withHeader("Authorization", equalTo("Bearer my-secret-token"))
                .withRequestBody(
                    equalToJson(
                        """
                    {
                      "response_format": {
                        "type": "json_schema",
                        "json_schema": {
                          "strict": true,
                          "name": "Car",
                          "schema": {
                            "type": "object",
                            "additionalProperties": false,
                            "properties": {
                              "make": {
                                "type": "string"
                              },
                              "model": {
                                "type": "string"
                              },
                              "horsePower": {
                                "type": "integer"
                              }
                            },
                            "required": [
                              "make",
                              "model",
                              "horsePower"
                            ]
                          }
                        }
                      },
                      "model": "latest-chatgpt",
                      "messages": [
                        {
                          "role": "system",
                          "content": "You are a car inventor. When prompted, create a new car."
                        },
                        {
                          "role": "user",
                          "content": "Create a powerful car with a confidence-inspiring model name. \nUse a well-respected manufacturer as the make."
                        }
                      ]
                    }
                    """,
                    ),
                ),
        )
    }

    @Test
    fun `throws on server error`() {
        data class Car(
            val make: String,
            val model: String,
            val horsePower: Int,
        )
        stubFor(
            post("/v1/chat/completions/")
                .willReturn(
                    ResponseDefinitionBuilder()
                        .withStatus(500)
                        .withBody("Something went wrong on the server side"),
                ),
        )
        expectThrows<HttpServerErrorException> {
            sut.performStructuredOutputQuery(
                OpenAIStructuredRequestData(
                    model = "latest-chatgpt",
                    messages =
                        listOf(
                            OpenAIMessage(
                                role = OpenAIMessage.Role.SYSTEM,
                                content = "You are a car inventor. When prompted, create a new car.",
                            ),
                            OpenAIMessage(
                                role = OpenAIMessage.Role.USER,
                                content =
                                    """
                                    Create a powerful car with a confidence-inspiring model name. 
                                    Use a well-respected manufacturer as the make.
                                    """.trimIndent(),
                            ),
                        ),
                    responseFormatClass = Car::class,
                ),
            )
        }.and { get { this.message }.isNotNull().contains("Something went wrong on the server side") }
    }

    @Test
    fun `throws on client error`() {
        data class Car(
            val make: String,
            val model: String,
            val horsePower: Int,
        )
        stubFor(
            post("/v1/chat/completions/")
                .willReturn(
                    ResponseDefinitionBuilder()
                        .withStatus(403)
                        .withBody("Something went wrong on the client side"),
                ),
        )
        expectThrows<HttpClientErrorException> {
            sut.performStructuredOutputQuery(
                OpenAIStructuredRequestData(
                    model = "latest-chatgpt",
                    messages =
                        listOf(
                            OpenAIMessage(
                                role = OpenAIMessage.Role.SYSTEM,
                                content = "You are a car inventor. When prompted, create a new car.",
                            ),
                            OpenAIMessage(
                                role = OpenAIMessage.Role.USER,
                                content =
                                    """
                                    Create a powerful car with a confidence-inspiring model name. 
                                    Use a well-respected manufacturer as the make.
                                    """.trimIndent(),
                            ),
                        ),
                    responseFormatClass = Car::class,
                ),
            )
        }.and { get { this.message }.isNotNull().contains("Something went wrong on the client side") }
    }
}

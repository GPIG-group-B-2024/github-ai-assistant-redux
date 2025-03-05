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
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest

@AiAssistantTest
@WireMockTest(httpPort = 3001)
class OpenAIClientTest {
    @Autowired
    private lateinit var sut: OpenAIClient

    @Test
    fun `smoke test`() {
        data class Car(val make: String, val model: String, val horsePower: Int)
        stubFor(
            post("/").willReturn(
                ok().withBody(
                    """
            {
            "make": "BMW",
            "model": "3-series",
            "horsePower": 100
            }
        """,
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
                            message = "You are a car inventor. When prompted, create a new car.",
                        ),
                        OpenAIMessage(
                            role = OpenAIMessage.Role.USER,
                            message =
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
            postRequestedFor(urlEqualTo("/"))
                .withHeader("Authorization", equalTo("Bearer my-secret-token"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                        "response_format": {
                          "type": "object",
                          "additionalProperties": false,
                          "required": ["make", "model", "horsePower"],
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
                          }
                        },
                        "model": "latest-chatgpt",
                        "messages": [
                          {
                            "role": "system",
                            "message": "You are a car inventor. When prompted, create a new car."
                          },
                          {
                            "role": "user",
                            "message": "Create a powerful car with a confidence-inspiring model name. \nUse a well-respected manufacturer as the make."
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
        data class Car(val make: String, val model: String, val horsePower: Int)
        stubFor(
            post("/").willReturn(
                ResponseDefinitionBuilder().withStatus(500)
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
                                message = "You are a car inventor. When prompted, create a new car.",
                            ),
                            OpenAIMessage(
                                role = OpenAIMessage.Role.USER,
                                message =
                                    """
                                    Create a powerful car with a confidence-inspiring model name. 
                                    Use a well-respected manufacturer as the make.
                                    """.trimIndent(),
                            ),
                        ),
                    responseFormatClass = Car::class,
                ),
            )
        }.and {
            get { this.message }.isNotNull().contains("Something went wrong on the server side")
        }
    }

    @Test
    fun `throws on client error`() {
        data class Car(val make: String, val model: String, val horsePower: Int)
        stubFor(
            post("/").willReturn(
                ResponseDefinitionBuilder().withStatus(403)
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
                                message = "You are a car inventor. When prompted, create a new car.",
                            ),
                            OpenAIMessage(
                                role = OpenAIMessage.Role.USER,
                                message =
                                    """
                                    Create a powerful car with a confidence-inspiring model name. 
                                    Use a well-respected manufacturer as the make.
                                    """.trimIndent(),
                            ),
                        ),
                    responseFormatClass = Car::class,
                ),
            )
        }.and {
            get { this.message }.isNotNull().contains("Something went wrong on the client side")
        }
    }
}

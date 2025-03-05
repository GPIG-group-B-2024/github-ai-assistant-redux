package uk.ac.york.gpig.teamb.aiassistant.llm.client

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
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.utils.types.toJsonSchema

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
                responseFormat = Car::class.toJsonSchema(),
            ),
            Car::class.java,
        )
        verify(
            postRequestedFor(urlEqualTo("/"))
                .withHeader("Authorization", equalTo("Bearer my-secret-token"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                        "responseFormat": {
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
}

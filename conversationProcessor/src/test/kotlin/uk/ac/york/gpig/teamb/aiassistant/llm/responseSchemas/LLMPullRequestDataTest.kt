package uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import uk.ac.york.gpig.teamb.aiassistant.testutils.assertions.isEqualToJson
import uk.ac.york.gpig.teamb.aiassistant.utils.types.toJsonSchema

class LLMPullRequestDataTest {
    @Test
    fun `smoke test`() {
        expectThat(LLMPullRequestData::class.toJsonSchema())
            .isEqualToJson(
                """
            {
                "type": "object",
                "additionalProperties": false,
                "properties": {
                    "pullRequestBody": {
                    "type": "string"
                    },
                    "pullRequestTitle": {
                    "type": "string"
                    },
                    "updatedFiles": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "properties": {
                        "type": {
                            "type": "string",
                            "enum": [
                            "MODIFY",
                            "CREATE",
                            "DELETE"
                            ]
                        },
                        "filePath": {
                            "type": "string"
                        },
                        "newContents": {
                            "type": "string"
                        }
                        },
                        "required": [
                        "type",
                        "filePath",
                        "newContents"
                        ]
                    }
                    }
                },
                "required": [
                    "pullRequestBody",
                    "pullRequestTitle",
                    "updatedFiles"
                ]
            }
            """.replace("\\s".toRegex(), ""),
            )
    }
}

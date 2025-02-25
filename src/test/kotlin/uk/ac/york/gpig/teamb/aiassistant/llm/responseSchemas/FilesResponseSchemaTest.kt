package uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import uk.ac.york.gpig.teamb.aiassistant.testutils.assertions.isEqualToJson
import uk.ac.york.gpig.teamb.aiassistant.utils.types.toJsonSchema

class FilesResponseSchemaTest {
    @Test
    fun `smoke test`() {
        expectThat(FilesResponseSchema::class.toJsonSchema()).isEqualToJson(
            """
            {
                "type":"object",
                "additionalProperties":false,
                "properties":{
                    "fileList":{
                        "type":"array","items":{"type":"string"}
                    }
                             },
                "required":["fileList"]
            }
            """.replace("\\s".toRegex(), ""),
        )
    }

    @Test
    fun `smoke test2`() {
        expectThat(LLMPullRequestData::class.toJsonSchema()).isEqualToJson(
            """
            {
            
                "type":"object",
                "additionalProperties":false,
                "properties":{"pullRequestBody":{"type":"string"},
                "pullRequestTitle":{"type":"string"},
                "updatedFiles":
                    {
                    "type":"array",
                    "items":{"type":"object",
                    "additionalProperties":false,
                    "properties":{"fullName":{"type":"string"},
                    "newContents":{"type":"string"}},
                    "required":["fullName","newContents"]}}},
                    "required":["pullRequestBody","pullRequestTitle","updatedFiles"]
                    }
            """.replace("\\s".toRegex(), ""),
        )
    }
}

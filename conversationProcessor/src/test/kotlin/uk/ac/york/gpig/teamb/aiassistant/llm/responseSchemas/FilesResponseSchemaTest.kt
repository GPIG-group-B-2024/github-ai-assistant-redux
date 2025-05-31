package uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import uk.ac.york.gpig.teamb.aiassistant.testutils.assertions.isEqualToJson
import uk.ac.york.gpig.teamb.aiassistant.utils.types.toJsonSchema

class FilesResponseSchemaTest {
    @Test
    fun `smoke test`() {
        expectThat(FilesResponseSchema::class.toJsonSchema())
            .isEqualToJson(
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
}

package uk.ac.york.gpig.teamb.aiassistant.utils

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import uk.ac.york.gpig.teamb.aiassistant.testutils.assertions.isEqualToJson
import uk.ac.york.gpig.teamb.aiassistant.utils.types.toJsonSchema

class JSONSchemaTest {
    @Test
    fun `smoke test`() {
        data class SimpleClass(
            val a: String,
            val b: Int,
            val c: Boolean,
        )
        expectThat(SimpleClass::class.toJsonSchema()).isEqualToJson(
            """
            {
                "type":"object",
                "additionalProperties":false,
                "properties":{
                    "a":{"type":"string"},
                    "b":{"type":"integer"},
                    "c":{"type":"boolean"}
                    },
                "required": ["a", "b", "c"]
            }
        """.replace("\\s".toRegex(), ""),
        )
    }

    @Test
    fun `can handle nested objects`() {
        data class Nested(val d: Boolean)

        data class NestedObject(
            val a: String,
            val b: Int,
            val c: Nested,
        )

        expectThat(NestedObject::class.toJsonSchema()).isEqualToJson(
            """
            {
              "type": "object",
              "additionalProperties": false,
              "required" : ["a", "b", "c"],
              "properties": {
                "a": {
                  "type": "string"
                },
                "b": {
                  "type": "integer"
                },
                "c": {
                  "type": "object",
                  "required": ["d"],
                  "additionalProperties": false,
                  "properties": {
                    "d": {
                      "type": "boolean"
                    }
                  }
                }
              }
            }
        """.replace("\\s".toRegex(), ""),
        )
    }

    @Test
    @DisplayName("can handle nested arrays") // test breaks if function name contains spaces
    fun handlesArrays() {
        data class Nested(val d: Boolean)

        data class NestedArray(
            val a: String,
            val b: Int,
            val c: List<Nested>,
        )

        expectThat(NestedArray::class.toJsonSchema()).isEqualToJson(
            """
            {
                "type": "object",
                "additionalProperties": false,
                "required": ["a", "b", "c"],
                "properties": {
                  "a": {
                    "type": "string"
                  },
                  "b": {
                    "type": "integer"
                  },
                  "c": {
                    "type": "array",
                    "items": {
                      "type": "object",
                      "required": ["d"],
                      "additionalProperties": false,
                      "properties": {
                        "d": {
                          "type": "boolean"
                        }
                      }
                    }
                  }
                }
            }
        """.replace("\\s".toRegex(), ""),
        )
    }
}

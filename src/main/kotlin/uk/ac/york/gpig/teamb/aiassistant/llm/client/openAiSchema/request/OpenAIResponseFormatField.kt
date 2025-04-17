package uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

/**
 * Corresponds to the `response_format` field attached to a POST request to the OpenAI API.
 *
 * Contains the following:
 * - response type (defaults to `json_schema`)
 * - `json_schema` object, containing:
 *     - the name of the schema
 *     - whether or not the schema is strict (defaults to `true`)
 *     - the schema itself i.e. the list of properties and their types
 */
data class OpenAIResponseFormatField(
    val type: String = "json_schema",
    @JsonProperty("json_schema") val jsonSchema: OpenAIJsonSchema,
) {
    data class OpenAIJsonSchema(
        val strict: Boolean = true,
        val name: String,
        val schema: JsonNode,
    )
}

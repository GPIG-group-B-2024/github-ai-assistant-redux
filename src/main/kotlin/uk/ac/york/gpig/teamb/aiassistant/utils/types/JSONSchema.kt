package uk.ac.york.gpig.teamb.aiassistant.utils.types

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchema
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchemaGenerator
import kotlin.reflect.KClass

/** Generate a JSON schema from a given Kotlin type */
fun <T : Any> KClass<T>.toJsonSchema(): JsonNode {
    val mapper = ObjectMapper()
    val schemaGenerator = JsonSchemaGenerator(mapper)
    val schema = schemaGenerator.generateSchema(this.java).cleanupForStructuredOutput()
    val schemaNode = mapper.valueToTree<JsonNode>(schema).markPropertiesAsRequired(mapper)
    return schemaNode
}

/**
 * Remove the `id` field and reject additional properties for this schema and any nested object
 * schemas.
 *
 * This prepares it for use with the structured output feature of ChatGPT and ensures the least
 * possible chances of unwanted data making it into the response.
 */
internal fun JsonSchema.cleanupForStructuredOutput(): JsonSchema =
    this.also {
        when {
            it.isObjectSchema -> {
                it.id = null // remove the id field (not needed)
                it.asObjectSchema().rejectAdditionalProperties()
                // ^ set `additionalProperties` to false (to prevent chatGPT from putting random fields
                // into its answer)
                it.asObjectSchema().properties.forEach { (_, childSchema) ->
                    childSchema.cleanupForStructuredOutput()
                }
                // ^ visit nested properties and check if any of them are objects/arrays too
            }
            it.isArraySchema ->
                it
                    .asArraySchema()
                    .items
                    .asSingleItems()
                    .schema
                    .cleanupForStructuredOutput()
            // ^ we found a nested array schema, check if its items are object schemas and if so, make
            // the changes
            // NOTE: assumes that arrays can only contain data of one type (fine for strongly typed
            // languages)
            else -> Unit
            // ^ the schema is neither, stop
        }
    }

/**
 * Mark all nested fields of an object as required.
 *
 * Because the jackson schema generator module doesn't generate v4 compliant schemas, this is a
 * workaround.
 *
 * This function will simply add all property names to a "required" array field as described in
 * their
 * [docs](https://platform.openai.com/docs/guides/structured-outputs#all-fields-must-be-required)
 */
internal fun JsonNode.markPropertiesAsRequired(mapper: ObjectMapper): JsonNode =
    this.also {
        when (
            it.get("type").textValue()
        ) { // this "type" field is guaranteed to be there as the node represents a
            // JSON schema
            "object" -> {
                // we are looking at an object schema
                val propertyNames =
                    mapper.valueToTree<JsonNode>(it.get("properties").properties().map { it.key })
                (it as ObjectNode).putIfAbsent("required", propertyNames)
                // ^ find all property names, convert them to a JSON array and add to the node
                it.get("properties").properties().forEach { it.value.markPropertiesAsRequired(mapper) }
                // ^ check if any nested properties need the same treatment (i.e. check if there are any
                // nested object schemas)
            }
            "array" -> it.get("items").markPropertiesAsRequired(mapper)
            // ^ check if the type of the array item is an object and add the "required" field if so
            else -> Unit // this is a primitive schema, stop
        }
    }

package uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.request

import uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.request.OpenAIResponseFormatField.OpenAIJsonSchema
import uk.ac.york.gpig.teamb.aiassistant.utils.types.toJsonSchema
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

data class OpenAIStructuredRequestData<TResponse : Any>(
    val responseFormatClass: KClass<TResponse>,
    val model: String,
    val messages: List<OpenAIMessage>,
) {
    fun toPostRequestBody() =
        OpenAIResponseFormatField(
            jsonSchema =
                OpenAIJsonSchema(
                    // use the simple name if available (it should be if the schema class is not anonymous.)
                    // otherwise, use the full JVM name
                    name =
                        this.responseFormatClass.run {
                            simpleName ?: jvmName
                        },
                    schema = this.responseFormatClass.toJsonSchema(),
                ),
        )
}

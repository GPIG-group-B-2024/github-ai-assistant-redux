package uk.ac.york.gpig.teamb.aiassistant.llm.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import uk.ac.york.gpig.teamb.aiassistant.utils.types.toJsonSchema
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

data class OpenAIMessage(
    val role: Role,
    val message: String,
) {
    /**
     * What kind of prompt this message is: system vs user.
     * */
    enum class Role {
        @JsonProperty("user")
        USER,

        @JsonProperty("system")
        SYSTEM,

        @JsonProperty("assistant")
        ASSISTANT,
    }
}

sealed class OpenAIRequestData {
    abstract val model: String
    abstract val messages: List<OpenAIMessage>
}

data class OpenAIStructuredRequestData<TResponse : Any>(
    val responseFormatClass: KClass<TResponse>,
    override val model: String,
    override val messages: List<OpenAIMessage>,
) : OpenAIRequestData() {
    fun toPostRequestBody() =
        OpenAIAPIResponseFormat(
            jsonSchema =
                OpenAIAPIResponseFormat.OpenAIJsonSchema(
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

data class OpenAIAPIResponseFormat(
    val type: String = "json_schema",
    @JsonProperty("json_schema")
    val jsonSchema: OpenAIJsonSchema,
) {
    data class OpenAIJsonSchema(
        val strict: Boolean = true,
        val name: String,
        val schema: JsonNode,
    )
}

data class OpenAIUnstructuredRequestData(
    override val model: String,
    override val messages: List<OpenAIMessage>,
) : OpenAIRequestData()

package uk.ac.york.gpig.teamb.aiassistant.testutils.assertions
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Suppress("UNCHECKED_CAST")
fun <T : JsonNode> Builder<T>.isEqualToJson(jsonString: String): Builder<T> {
    val mapper = ObjectMapper()
    val first: JsonNode = this.subject
    val second: JsonNode = mapper.readTree(jsonString)
    return expectThat(first).isEqualTo(second) as Builder<T>
}

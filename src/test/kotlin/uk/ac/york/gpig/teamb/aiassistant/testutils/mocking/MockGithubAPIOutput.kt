package uk.ac.york.gpig.teamb.aiassistant.testutils.mocking

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object MockGithubAPIOutput {
    /**
     * Construct a mock github API response simulating a request for the contents of a given file.
     *
     * Example output taken from
     * https://docs.github.com/en/rest/repos/contents?apiVersion=2022-11-28#get-repository-content
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun mockGithubAPIBlob(
        filename: String,
        expectedContent: String,
    ): String {
        val objectMapper = ObjectMapper()
        // grab the json file and parse it into a Json node
        val initialResponseString =
            File("src/test/resources/wiremock/github-api/file-contents-output.json").readText()
        val initialResponseJson = objectMapper.readValue(initialResponseString, JsonNode::class.java)

        val expectedContentEncoded = Base64.encode(expectedContent.toByteArray())
        // modify the node with our desired info
        (initialResponseJson as ObjectNode).put("content", expectedContentEncoded)
        initialResponseJson.put("path", filename)
        return objectMapper.writeValueAsString(initialResponseJson)
    }
}

package uk.ac.york.gpig.teamb.aiassistant.llm

import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.ac.york.gpig.teamb.aiassistant.database.C4Manager
import uk.ac.york.gpig.teamb.aiassistant.llm.client.LLMPullRequestData
import uk.ac.york.gpig.teamb.aiassistant.llm.client.OpenAIClient
import uk.ac.york.gpig.teamb.aiassistant.llm.client.OpenAIMessage
import uk.ac.york.gpig.teamb.aiassistant.llm.client.OpenAIStructuredRequestData
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload.Issue
import uk.ac.york.gpig.teamb.aiassistant.utils.types.toJsonSchema

// Model version that supports the "structured output" feature
const val CHATGPT_VERSION = "gpt-4o-2024-08-06"

/**
 * Handles interactions with the OpenAI API
 * */
@Service
class LLMManager(
    @Autowired
    private val client: OpenAIClient,
    @Autowired
    private val c4Manager: C4Manager,
) {
    fun produceIssueSolution(
        repoName: String,
        issue: Issue,
    ): LLMPullRequestData {
        val c4Data = c4Manager.gitRepoToStructurizrDsl(repoName) // TODO: incorporate this into the prompt

        val rawJson =
            client.performStructuredOutputQuery(
                OpenAIStructuredRequestData(
                    model = CHATGPT_VERSION,
                    messages =
                        listOf(
                            OpenAIMessage(
                                role = OpenAIMessage.Role.SYSTEM,
                                message = """
                        You are a software developer working on a repository called (repo name here).
                        You must receive an issue description and some helpful context and provide a fix.
                        You must adhere to the output schema provided.
                        **Important** when sending your solutions, make sure to include the __whole__ file, 
                        not just the changes that must be made to it.
                    
                    """,
                                // ^ TODO: develop the actual prompt
                            ),
                            OpenAIMessage(
                                role = OpenAIMessage.Role.USER,
                                message = """
                        Here is my issue:
                        (issue description here)
                        
                        Here is a C4 representation of my code base:
                        (C4 here)
                        
                        
                    """,
                                // ^ TODO: same here
                            ),
                        ),
                    responseFormat = LLMPullRequestData::class.toJsonSchema(),
                ),
            ) ?: throw Exception("Could not parse LLM output. Check the prompt or try again")
        return Gson().fromJson(rawJson, LLMPullRequestData::class.java)
    }
}

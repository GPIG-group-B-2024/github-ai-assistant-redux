package uk.ac.york.gpig.teamb.aiassistant.llm

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.ac.york.gpig.teamb.aiassistant.database.c4.C4Manager
import uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.LLMConversationManager
import uk.ac.york.gpig.teamb.aiassistant.enums.ConversationStatus
import uk.ac.york.gpig.teamb.aiassistant.llm.client.OpenAIClient
import uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.request.OpenAIMessage
import uk.ac.york.gpig.teamb.aiassistant.llm.client.openAiSchema.request.OpenAIStructuredRequestData
import uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas.FilesResponseSchema
import uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas.LLMPullRequestData
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload.Issue
import uk.ac.york.gpig.teamb.aiassistant.vcs.VCSManager
import java.util.UUID

/** Handles interactions with the OpenAI API */
@Service
class LLMManager(
    @Value("\${app_settings.chatgpt_version:gpt-4o-2024-08-06}") private val chatGptVersion: String,
    private val client: OpenAIClient,
    private val c4Manager: C4Manager,
    private val conversationManager: LLMConversationManager,
    private val transactionTemplate: TransactionTemplate,
    private val vscManager: VCSManager,
    private val gson: Gson,
) {
    internal fun getSystemPrompt(repoName: String) =
        OpenAIMessage(
            OpenAIMessage.Role.SYSTEM,
            """
            |You are a software engineer working on a repo called **$repoName**.
            |
            |You will be provided with an issue description, the repo's file tree and its model in the structurizr modelling framework.
            |
            |Your task is to respond to user messages with your best attempts at solving their issue.
            """.trimMargin(),
        )

    internal fun getRepoInfoMessage(
        repoName: String,
        issue: Issue,
    ) = OpenAIMessage(
        OpenAIMessage.Role.USER,
        """
        |Here is some information about the repository:
        
        |   * C4 model: 
        |       ```
        |       ${c4Manager.gitRepoToStructurizrDsl(repoName)}
        |       ```
        |
        |   * File tree: 
        |       ```
        |        ${vscManager.retrieveFileTree(repoName)}
        |       ```
        |
        |   * Issue title: ${issue.title}
        |
        |   * Issue body: ${issue.body}
        |
        |Your first task is to give me the list of files you need to inspect in full before creating your solution.
        |You should pick the files that you will need to either know in full or modify when making your fix to the issue.
        |Respond with a single list of strings, where each string represents a path to the file from the **repository root**
        |
        |**IMPORTANT** Make sure you only request the files present in the file tree I sent over.
        """.trimMargin(),
    )

    internal fun getPullRequestMessage(
        conversationId: UUID,
        repoName: String,
        requiredFiles: FilesResponseSchema,
    ): OpenAIMessage {
        val fileBlobs =
            try {
                vscManager.fetchFileBlobs(repoName, requiredFiles.fileList)
            } catch (e: Exception) {
                logger.error("Failed to retrieve file blobs for conversation $conversationId")
                conversationManager.updateConversationStatus(conversationId, ConversationStatus.FAILED)
                throw Exception("Failed to obtain file blobs for conversation $conversationId: $e")
            }
        return OpenAIMessage(
            OpenAIMessage.Role.USER,
            """
            |Great. I am now sending you the files you requested. Your task is to now produce a pull request. 
            |Your response should consist of:
            |   * Pull request title
            |   * Pull request body
            |   * Your changes: **IMPORTANT** - you must send the updated files in __full__, they must be able to overwrite 
            |   the original file without breaking any functionality not affected by the pull request.
            |
            |Here are the files you requested:
            |${
                fileBlobs.joinToString("\n\n") {
                    """
                    ## ${it.path}
                    ```
                    ${it.contents}
                    ```
                    ---
                    """
                }
            }
            """.trimMargin(),
        )
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Attempt to perform a structured output query, running some code on failure before re-throwing
     * the exception
     *
     * @param onFailure The code to run when the request fails. Takes in the exception as the
     *   argument. Note: must never return i.e. must re-throw the exception in the end of execution
     */
    private fun <TResponse : Any> trySendMessageOrElse(
        // the message to send
        message: OpenAIStructuredRequestData<TResponse>,
        // what to do on failure - re-throwing the exception in the end (e.g. log the error)
        onFailure: (Exception) -> Nothing,
    ): TResponse =
        try {
            client.performStructuredOutputQuery(message)
        } catch (e: Exception) {
            onFailure(e)
        }

    /** Walk through the conversation flow outlined in the issue description */
    fun produceIssueSolution(
        repoName: String,
        issue: Issue,
    ): LLMPullRequestData {
        // step 1: send in the system prompt, gather data about repo + send in the issue description
        val systemPrompt = getSystemPrompt(repoName)

        val initialMessage = getRepoInfoMessage(repoName, issue)

        val firstRequestData =
            OpenAIStructuredRequestData(
                model = chatGptVersion,
                responseFormatClass = FilesResponseSchema::class,
                messages = listOf(systemPrompt, initialMessage),
            )

        // We have enough data to store the start of the conversation into the database:
        val conversationId =
            transactionTemplate.execute {
                val conversationId =
                    conversationManager.initConversationWithFirstMessage(
                        c4Manager.getRepoId(repoName),
                        issue.number,
                        OpenAIMessage.Role.SYSTEM,
                        systemPrompt.content,
                    )
                conversationManager.addMessageToConversation(conversationId, initialMessage)
                conversationId
            }
                ?: throw Exception(
                    "Could not initiate conversation about issue ${issue.number} in repo $repoName",
                )

        // make first request: we should get a list of files back
        val filesToInspectInFull =
            trySendMessageOrElse(firstRequestData) { e ->
                logger.error("Marking conversation $conversationId as failed after 1st user message")
                conversationManager.updateConversationStatus(conversationId, ConversationStatus.FAILED)
                throw Exception(
                    "LLM query in conversation $conversationId failed with error after 1st user message: $e",
                )
            }

        // store chatGPT's response into the database
        val chatGptResponseMessage =
            OpenAIMessage(OpenAIMessage.Role.ASSISTANT, gson.toJson(filesToInspectInFull))
        conversationManager.addMessageToConversation(conversationId, chatGptResponseMessage)

        // send the requested files and ask for the final output - the pull request data

        val secondMessage = getPullRequestMessage(conversationId, repoName, filesToInspectInFull)

        conversationManager.addMessageToConversation(conversationId, secondMessage)

        val pullRequestData =
            trySendMessageOrElse(
                OpenAIStructuredRequestData(
                    model = chatGptVersion,
                    responseFormatClass = LLMPullRequestData::class,
                    messages = listOf(systemPrompt, initialMessage, chatGptResponseMessage, secondMessage),
                ),
            ) { e ->
                logger.error("Marking conversation $conversationId as failed after 2nd user message")
                conversationManager.updateConversationStatus(conversationId, ConversationStatus.FAILED)
                throw Exception(
                    "LLM query in conversation $conversationId failed with error after 2nd user message: $e ",
                )
            }
        // we have received the pull request data. Write the remaining message to the database, mark the
        // conversation as complete and return the data.
        transactionTemplate.execute {
            conversationManager.addMessageToConversation(
                conversationId,
                OpenAIMessage(OpenAIMessage.Role.ASSISTANT, gson.toJson(pullRequestData)),
            )
            conversationManager.updateConversationStatus(conversationId, ConversationStatus.COMPLETED)
        }

        return pullRequestData
    }
}

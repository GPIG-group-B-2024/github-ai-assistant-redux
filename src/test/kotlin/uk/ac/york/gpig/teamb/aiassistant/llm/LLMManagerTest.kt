package uk.ac.york.gpig.teamb.aiassistant.llm

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.jooq.DSLContext
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.withNotNull
import uk.ac.york.gpig.teamb.aiassistant.enums.ConversationStatus
import uk.ac.york.gpig.teamb.aiassistant.enums.LlmMessageRole
import uk.ac.york.gpig.teamb.aiassistant.enums.MemberType
import uk.ac.york.gpig.teamb.aiassistant.llm.client.OpenAIClient
import uk.ac.york.gpig.teamb.aiassistant.llm.client.PromptRefusedException
import uk.ac.york.gpig.teamb.aiassistant.llm.client.PromptTooLongException
import uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas.LLMPullRequestData
import uk.ac.york.gpig.teamb.aiassistant.tables.references.CONVERSATION_MESSAGE
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_CONVERSATION
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_MESSAGE
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.testutils.assertions.isEqualToJson
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.GitRepoBuilder.Companion.gitRepo
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.MemberBuilder.Companion.member
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.RelationshipBuilder.Companion.relationship
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.WorkspaceBuilder.Companion.workspace
import uk.ac.york.gpig.teamb.aiassistant.testutils.mocking.MockGithubAPIOutput.mockGithubAPIBlob
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload.Issue
import uk.ac.york.gpig.teamb.aiassistant.vcs.facades.github.GitHubFacade
import java.io.File
import java.util.UUID

@AiAssistantTest
class LLMManagerTest {
    companion object {
        @RegisterExtension
        val mockGithubAPI: WireMockExtension =
            WireMockExtension.newInstance().options(wireMockConfig().port(3000)).build()

        @RegisterExtension
        val mockOpenAIAPI: WireMockExtension =
            WireMockExtension.newInstance().options(wireMockConfig().port(3001)).build()
    }

    @Autowired private lateinit var sut: LLMManager

    @Autowired private lateinit var ctx: DSLContext

    @SpykBean private lateinit var gitHubFacade: GitHubFacade

    @SpykBean private lateinit var client: OpenAIClient

    @Nested
    inner class ProduceIssueSolutionTest {
        /**
         * A utility function that:
         * - creates a git repo
         * - creates a c4 representation
         * - creates an issue
         * - mocks both the github and openAI's API
         *
         * @return the id of the created repo
         */
        private fun prepareTestEnv(repoName: String): UUID {
            // step 1: create a repo
            val workspaceId = UUID.randomUUID()
            val repoId = UUID.randomUUID()
            gitRepo {
                fullName = repoName
                id = repoId
                workspace {
                    this.name = "weather-app"
                    this.description = "fetches weather data from an external API and presents it nicely"
                    this.id = workspaceId
                }
            }.create(ctx)
            // step 2: store a c4 model into the database
            val rootMemberId = UUID.randomUUID()
            member {
                this.workspace { this.id = workspaceId }
                this.type = MemberType.SOFTWARE_SYSTEM
                this.name = "Software system"
                this.id = rootMemberId
            }.create(ctx)
            relationship {
                this.workspace { this.id = workspaceId }
                this.startMember {
                    this.workspace { this.id = workspaceId }
                    this.type = MemberType.CONTAINER
                    this.name = "Web app"
                    this.parentId = rootMemberId
                }
                this.endMember {
                    this.workspace { this.id = workspaceId }
                    this.type = MemberType.CONTAINER
                    this.name = "Database"
                    this.parentId = rootMemberId
                }
                this.description = "Reads from and writes to"
            }.create(ctx)
            // step 3.1: mock github endpoints
            // 3.1.1: mock the repo information endpoint
            every { gitHubFacade.generateInstallationToken() } returns "my-fancy-token"
            val getRepoOutput =
                File("src/test/resources/wiremock/github-api/get-repo-output.json").readText()
            mockGithubAPI.stubFor(
                get(urlEqualTo("/repos/my-owner/my-test-repo")).willReturn(ok().withBody(getRepoOutput)),
            )
            mockGithubAPI.stubFor(
                get(urlMatching("/repos/my-owner/my-test-repo/git/trees/main.*"))
                    .willReturn(
                        ok()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                File("src/test/resources/wiremock/github-api/get-repo-tree-output.json").readText(),
                            ),
                    ),
            )
            // 3.1.2: mock the /contents endpoints (i.e. create mock base64 outputs)
            listOf("src/weather_app/main.py", "test/weather_app/test_main.py").forEach { path ->
                mockGithubAPI.stubFor(
                    get("/repos/my-owner/my-test-repo/contents/$path?ref=HEAD")
                        .willReturn(
                            ok()
                                .withBody(
                                    mockGithubAPIBlob(
                                        path,
                                        """
                                # i am $path and this is some cool python code:
                                def greet(name):
                                    return f'Hello, {name}!'
                                """,
                                    ),
                                ),
                        ),
                )
            }
            // step 3.2: mock openAI endpoints
            // first, respond with the list of files
            mockOpenAIAPI.stubFor(
                post(urlMatching("/v1/chat/completions.*"))
                    .inScenario("ChatGPT conversation")
                    .whenScenarioStateIs(STARTED)
                    .willReturn(
                        ok()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                File("src/test/resources/wiremock/openai-api/list-of-files-response.json")
                                    .readText(),
                            ),
                    ).willSetStateTo("List of files received"),
            )
            // then respond with PR data
            mockOpenAIAPI.stubFor(
                post(urlMatching("/v1/chat/completions.*"))
                    .inScenario("ChatGPT conversation")
                    .whenScenarioStateIs("List of files received")
                    .willReturn(
                        ok()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                File("src/test/resources/wiremock/openai-api/pr-data-response.json").readText(),
                            ),
                    ),
            )
            return repoId
        }

        @Test
        fun `records chatGPT conversation and all messages`() {
            val repoName = "my-owner/my-test-repo"
            val repoId = prepareTestEnv(repoName)
            sut.produceIssueSolution(
                repoName,
                Issue(
                    title = "Add function to greet the user",
                    body = "Create a function that, given the user's name, greets them.",
                    id = 1L,
                    number = 1,
                ),
            )
            // check that the conversation has been created
            expectThat(ctx.selectFrom(LLM_CONVERSATION).fetch())
                .hasSize(1)
                .get { this.first() }
                .and {
                    get { this.issueId }.isEqualTo(1)
                    get { this.repoId }.isEqualTo(repoId)
                    get { this.status }.isEqualTo(ConversationStatus.COMPLETED)
                }
            // check that there are 5 recorded messages (system prompt, 1st user, 1st response, 2nd user,
            // 2nd response)
            expectThat(
                ctx
                    .selectFrom(LLM_MESSAGE)
                    .orderBy(LLM_MESSAGE.CREATED_AT)
                    .fetch()
                    .map { it.role },
            ).containsExactly(
                LlmMessageRole.SYSTEM,
                LlmMessageRole.USER,
                LlmMessageRole.ASSISTANT,
                LlmMessageRole.USER,
                LlmMessageRole.ASSISTANT,
            )
            // check that the messages are linked to the conversation
            val conversationId =
                ctx
                    .select(LLM_CONVERSATION.ID)
                    .from(LLM_CONVERSATION)
                    .fetchOne()
                    ?.get(LLM_CONVERSATION.ID)!!
            expectThat(ctx.selectFrom(CONVERSATION_MESSAGE).fetch()).hasSize(5).all {
                get { this.conversationId }.isEqualTo(conversationId)
            }
        }

        @Test
        fun `calls github and openAI API's with correct parameters`() {
            val repoName = "my-owner/my-test-repo"
            prepareTestEnv(repoName)
            sut.produceIssueSolution(
                repoName,
                Issue(
                    title = "Add function to greet the user",
                    body = "Create a function that, given the user's name, greets them.",
                    id = 1L,
                    number = 1,
                ),
            )
            // check that the github API was used to retrieve the repo tree

            mockGithubAPI.verify(
                getRequestedFor(urlMatching("/repos/my-owner/my-test-repo/git/trees/main.*"))
                    .withQueryParam("recursive", equalTo("1")),
            )

            // check that the openAI API was used twice

            val openAIRequests =
                mockOpenAIAPI.findAll(postRequestedFor(urlMatching("/v1/chat/completions.*")))
            expectThat(openAIRequests).hasSize(2).and {
                // first request has the system prompt AND the information about the repo
                get { this[0].bodyAsString }
                    .contains("You are a software engineer working on a repo called **$repoName**.")
                    .contains("Here is some information about the repository:")
                // second one requests the solution
                get { this[1].bodyAsString }.contains("Your task is to now produce a pull request.")
            }
        }

        @Test
        fun `parses final output into pull request data class`() {
            val repoName = "my-owner/my-test-repo"
            prepareTestEnv(repoName)
            val result =
                sut.produceIssueSolution(
                    repoName,
                    Issue(
                        title = "Add function to greet the user",
                        body = "Create a function that, given the user's name, greets them.",
                        id = 1L,
                        number = 1,
                    ),
                )
            val objectMapper = ObjectMapper()
            // see resources/openai-api/pr-data-response.json
            expectThat(result)
                .isA<LLMPullRequestData>()
                .get { objectMapper.valueToTree<JsonNode>(this) }
                .isEqualToJson(
                    """
                    {
                      "pullRequestBody": "I have made some changes to address the issue",
                      "pullRequestTitle": "Create greeter function",
                      "updatedFiles": [
                        {
                          "type": "CREATE",
                          "filePath": "src/weather_app/main.py",
                          "newContents": "def greet(name):\n\tprint(f'hello {name}')"
                        },
                        {
                          "type": "CREATE",
                          "filePath": "test/weather_app/test_main.py",
                          "newContents": "assert(greet('steve') == 'hello steve')"
                        }
                      ]
                    }
                    """.trimIndent(),
                )
        }

        @Test
        fun <T> `marks conversation as failed if first user message unsuccessful`() {
            every<T> { client.performStructuredOutputQuery(any()) } throws
                PromptRefusedException("naughty naughty")
            val repoName = "my-owner/my-test-repo"
            prepareTestEnv(repoName)
            expectThrows<Exception> {
                sut.produceIssueSolution(
                    repoName,
                    Issue(
                        title = "Add function to greet the user",
                        body = "Create a function that, given the user's name, greets them.",
                        id = 1L,
                        number = 1,
                    ),
                )
            }.and { get { this.message }.isNotNull().contains("after 1st user message") }
            // check that conversation has been created but status is `failed`
            val foundConversations = ctx.selectFrom(LLM_CONVERSATION).fetch()
            expectThat(foundConversations).hasSize(1).and {
                get { this.first().status }.isEqualTo(ConversationStatus.FAILED)
            }

            // check that there are 2 recorded messages (system prompt, 1st user, FAIL)
            expectThat(
                ctx
                    .selectFrom(LLM_MESSAGE)
                    .orderBy(LLM_MESSAGE.CREATED_AT)
                    .fetch()
                    .map { it.role },
            ).containsExactly(LlmMessageRole.SYSTEM, LlmMessageRole.USER)
        }

        @Test
        fun <T> `marks conversation as failed if second user message unsuccessful`() {
            every<T> { client.performStructuredOutputQuery(any()) } answers
                {
                    callOriginal()
                } andThenThrows
                PromptTooLongException("naughty naughty")
            val repoName = "my-owner/my-test-repo"
            prepareTestEnv(repoName)
            expectThrows<Exception> {
                sut.produceIssueSolution(
                    repoName,
                    Issue(
                        title = "Add function to greet the user",
                        body = "Create a function that, given the user's name, greets them.",
                        id = 1L,
                        number = 1,
                    ),
                )
            }.and { get { this.message }.isNotNull().contains("after 2nd user message") }
            // check that conversation has been created but status is `failed`
            val foundConversations = ctx.selectFrom(LLM_CONVERSATION).fetch()
            expectThat(foundConversations).hasSize(1).and {
                get { this.first().status }.isEqualTo(ConversationStatus.FAILED)
            }

            // check that there are 4 recorded messages (system prompt, 1st user, assistant, 2nd user,
            // FAIL)
            expectThat(
                ctx
                    .selectFrom(LLM_MESSAGE)
                    .orderBy(LLM_MESSAGE.CREATED_AT)
                    .fetch()
                    .map { it.role },
            ).containsExactly(
                LlmMessageRole.SYSTEM,
                LlmMessageRole.USER,
                LlmMessageRole.ASSISTANT,
                LlmMessageRole.USER,
            )
        }

        @Test
        fun `attaches repo name to system prompt`() {
            val repoName = "my-owner/my-test-repo"
            val repoId = prepareTestEnv(repoName)
            sut.produceIssueSolution(
                repoName,
                Issue(
                    title = "Add function to greet the user",
                    body = "Create a function that, given the user's name, greets them.",
                    id = 1L,
                    number = 1,
                ),
            )
            // check that the conversation has been created
            expectThat(ctx.selectFrom(LLM_CONVERSATION).fetch())
                .hasSize(1)
                .get { this.first() }
                .and {
                    get { this.issueId }.isEqualTo(1)
                    get { this.repoId }.isEqualTo(repoId)
                    get { this.status }.isEqualTo(ConversationStatus.COMPLETED)
                }
            // get the system prompt - there is only ever one per conversation

            val systemPrompt =
                ctx.selectFrom(LLM_MESSAGE).where(LLM_MESSAGE.ROLE.eq(LlmMessageRole.SYSTEM)).fetchOne()

            expectThat(systemPrompt?.content).withNotNull {
                this.isEqualTo(
                    """
              |You are a software engineer working on a repo called **my-owner/my-test-repo**.
              |
              |You will be provided with an issue description, the repo's file tree and its model in the structurizr modelling framework.
              |
              |Your task is to respond to user messages with your best attempts at solving their issue.
                    """.trimMargin(),
                )
            }
        }

        @Test
        fun `attaches issue data and C4 notation to the first user message`() {
            val repoName = "my-owner/my-test-repo"
            val repoId = prepareTestEnv(repoName)
            sut.produceIssueSolution(
                repoName,
                Issue(
                    title = "Add function to greet the user",
                    body = "Create a function that, given the user's name, greets them.",
                    id = 1L,
                    number = 1,
                ),
            )
            // check that the conversation has been created
            expectThat(ctx.selectFrom(LLM_CONVERSATION).fetch())
                .hasSize(1)
                .get { this.first() }
                .and {
                    get { this.issueId }.isEqualTo(1)
                    get { this.repoId }.isEqualTo(repoId)
                    get { this.status }.isEqualTo(ConversationStatus.COMPLETED)
                }
            // get the message that is meant to have the initial data in (first message with the USER
            // role)

            val repoInfoMessage =
                ctx
                    .selectFrom(LLM_MESSAGE)
                    .where(LLM_MESSAGE.ROLE.eq(LlmMessageRole.USER))
                    .orderBy(LLM_MESSAGE.CREATED_AT.asc())
                    .limit(1)
                    .fetchOne()

            expectThat(repoInfoMessage?.content).withNotNull {
                this.isEqualTo(
                    """
              |Here is some information about the repository:
              |        
              |   * C4 model: 
              |       ```
              |       workspace "weather-app" "fetches weather data from an external API and presents it nicely"{
              |   model {
              |       Software-system = softwareSystem "Software system" "handles incoming HTTP requests"{
              |		Web-app = container "Web app" "handles incoming HTTP requests"
              |		Database = container "Database" "handles incoming HTTP requests"
              |		}
              |       Web-app -> Database "Reads from and writes to"
              |   }
              |}
              |       ```
              |
              |   * File tree: 
              |       ```
              |        README.md
              |greeter/greeting.py
              |math/addition.py
              |math/subtraction.py
              |math/utils/is_three.py
              |       ```
              |
              |   * Issue title: Add function to greet the user
              |
              |   * Issue body: Create a function that, given the user's name, greets them.
              |
              |Your first task is to give me the list of files you need to inspect in full before creating your solution.
              |You should pick the files that you will need to either know in full or modify when making your fix to the issue.
              |Respond with a single list of strings, where each string represents a path to the file from the **repository root**
                    """.trimMargin(),
                )
            }
        }

        @Test
        fun `attaches github file blobs to the second user message`() {
            val repoName = "my-owner/my-test-repo"
            val repoId = prepareTestEnv(repoName)
            sut.produceIssueSolution(
                repoName,
                Issue(
                    title = "Add function to greet the user",
                    body = "Create a function that, given the user's name, greets them.",
                    id = 1L,
                    number = 1,
                ),
            )
            // check that the conversation has been created
            expectThat(ctx.selectFrom(LLM_CONVERSATION).fetch())
                .hasSize(1)
                .get { this.first() }
                .and {
                    get { this.issueId }.isEqualTo(1)
                    get { this.repoId }.isEqualTo(repoId)
                    get { this.status }.isEqualTo(ConversationStatus.COMPLETED)
                }
            // get the message that is meant to have the file blobs in (second and last message with the
            // USER role)

            val fileBlobMessage =
                ctx
                    .selectFrom(LLM_MESSAGE)
                    .where(LLM_MESSAGE.ROLE.eq(LlmMessageRole.USER))
                    .orderBy(LLM_MESSAGE.CREATED_AT.desc())
                    .limit(1)
                    .fetchOne()
            expectThat(fileBlobMessage?.content).withNotNull {
                this.isEqualTo(
                    """
                |Great. I am now sending you the files you requested. Your task is to now produce a pull request. 
                |Your response should consist of:
                |   * Pull request title
                |   * Pull request body
                |   * Your changes: **IMPORTANT** - you must send the updated files in __full__, they must be able to overwrite 
                |   the original file without breaking any functionality not affected by the pull request.
                |
                |Here are the files you requested:
                |
                |                    ## src/weather_app/main.py
                |                    ```
                |                    
                |                                # i am src/weather_app/main.py and this is some cool python code:
                |                                def greet(name):
                |                                    return f'Hello, {name}!'
                |                                
                |                    ```
                |                    ---
                |                    
                |
                |
                |                    ## test/weather_app/test_main.py
                |                    ```
                |                    
                |                                # i am test/weather_app/test_main.py and this is some cool python code:
                |                                def greet(name):
                |                                    return f'Hello, {name}!'
                |                                
                |                    ```
                |                    ---
                |                    
                    """.trimMargin(),
                )
            }
        }
    }
}

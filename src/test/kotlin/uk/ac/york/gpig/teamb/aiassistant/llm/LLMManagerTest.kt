package uk.ac.york.gpig.teamb.aiassistant.llm

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.york.gpig.teamb.aiassistant.enums.MemberType
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.GitRepoBuilder.Companion.gitRepo
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.MemberBuilder.Companion.member
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.RelationshipBuilder.Companion.relationship
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.WorkspaceBuilder.Companion.workspace
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload.Issue
import uk.ac.york.gpig.teamb.aiassistant.vcs.facades.github.GitHubFacade
import java.io.File
import java.util.UUID

@AiAssistantTest
class LLMManagerTest {
    companion object {
        @RegisterExtension
        val mockGithubAPI =
            WireMockExtension.newInstance().options(
                wireMockConfig().port(3000),
            ).build()

        @RegisterExtension
        val mockOpenAIAPI =
            WireMockExtension.newInstance().options(
                wireMockConfig().port(3001),
            ).build()
    }

    @Autowired
    private lateinit var sut: LLMManager

    @Autowired
    private lateinit var ctx: DSLContext

    @SpykBean
    private lateinit var gitHubFacade: GitHubFacade

    /**
     * This is quite a large test that steps through the entire PR workflow.
     *
     * The plan is as follows:
     *  - create a git repo
     *  - create a c4 representation
     *  - create an issue
     *  - mock both the github and openAI's API
     *  - test that the final produced solution is parseable
     *
     * */
    @Test
    fun `happy path test`() {
        // step 1: create a repo
        val repoName = "my-owner/my-test-repo"
        val workspaceId = UUID.randomUUID()
        gitRepo {
            fullName = repoName
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
        every { gitHubFacade.generateInstallationToken() } returns "my-fancy-token"
        val getRepoOutput = File("src/test/resources/wiremock/github-api/get-repo-output.json").readText()
        mockGithubAPI.stubFor(get(urlEqualTo("/repos/my-owner/my-test-repo")).willReturn(ok().withBody(getRepoOutput)))
        mockGithubAPI.stubFor(
            get(
                urlMatching("/repos/my-owner/my-test-repo/git/trees/main.*"),
            ).willReturn(
                ok()
                    .withBody(File("src/test/resources/wiremock/github-api/get-repo-tree-output.json").readText()),
            ),
        )
        // step 3.2: mock openAI endpoints
        // first, respond with the list of files
        mockOpenAIAPI.stubFor(
            post(urlMatching(".*"))
                .inScenario("ChatGPT conversation")
                .whenScenarioStateIs(STARTED)
                .willReturn(ok().withBody("hello"))
                .willSetStateTo("List of files received"),
        )
        // then respond with PR data
        mockOpenAIAPI.stubFor(
            post(urlMatching("/v1/chat/completions.*"))
                .inScenario("ChatGPT conversation")
                .whenScenarioStateIs("List of files received")
                .willReturn(ok().withBody("byebye")),
        )
        sut.produceIssueSolution(
            repoName,
            Issue(
                title = "Add function to greet the user",
                body = "Create a function that, given the user's name, greets them.",
                id = 1L,
                number = 1,
            ),
        )
    }
}

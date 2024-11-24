package uk.ac.york.gpig.teamb.aiassistant.vcs.facades.github

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.junit.jupiter.api.Test
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import java.io.File

@AiAssistantTest
@WireMockTest(httpPort = 3000)
class GitHubFacadeTest {
    @SpykBean // spy instead of mock because we want some real methods to run
    private lateinit var sut: GitHubFacade

    @Test
    fun `creates a pull request with the right parameters`() {
        every { sut.generateInstallationToken() } returns "my-fancy-token" // we don't actually need the token here, mock it.

        // mock github API output: this is basically an exact copy of an example response from the docs page, but with the owner and repo name changed
        val getRepoOutput = File("src/test/resources/wiremock/get-repo-output.json").readText()
        val createPROutput = File("src/test/resources/wiremock/create-pull-request-output.json").readText()
        // ^ same as above, except we do not care *at all* what the output is, we only need it for the underlying github library to run without exceptions
        stubFor(get("/repos/my-owner/my-test-repo").willReturn(ok().withBody(getRepoOutput)))
        stubFor(
            post(
                "/repos/my-owner/my-test-repo/pulls",
            ).willReturn(ResponseDefinitionBuilder.responseDefinition().withStatus(201).withBody(createPROutput)),
        )
        sut.createPullRequest(
            "my-owner/my-test-repo",
            "main",
            "my-branch",
            "My title",
            "My description",
        )
        // verify the github library attempted to make a PR with the correct data
        verify(
            postRequestedFor(urlEqualTo("/repos/my-owner/my-test-repo/pulls")).withRequestBody(
                equalToJson(
                    """
                    {
                      "head" : "my-branch",
                      "draft" : false,
                      "maintainer_can_modify" : true,
                      "title" : "My title",
                      "body" : "My description",
                      "base" : "main"
                    }
                    """,
                ),
            ),
        )
    }

    @Test
    fun `writes a comment on the correct issue`() {
        every { sut.generateInstallationToken() } returns "my-fancy-token"
        // mock github API output: this is basically an exact copy of an example response from the docs page, but with the owner and repo name changed
        val getRepoOutput = File("src/test/resources/wiremock/get-repo-output.json").readText()
        val getIssueOutput = File("src/test/resources/wiremock/get-issue-output.json").readText()
        val createCommentOutput = File("src/test/resources/wiremock/create-comment-output.json").readText()
        // ^ same as above, except we do not care *at all* what the output is, we only need it for the underlying github library to run without exceptions
        stubFor(get("/repos/my-owner/my-test-repo").willReturn(ok().withBody(getRepoOutput)))
        stubFor(get("/repos/my-owner/my-test-repo/issues/5").willReturn(ok().withBody(getIssueOutput)))
        stubFor(
            post(
                "/repos/my-owner/my-test-repo/issues/5/comments",
            ).willReturn(ResponseDefinitionBuilder.responseDefinition().withStatus(201).withBody(createCommentOutput)),
        )

        // Act
        sut.createComment(
            "my-owner/my-test-repo",
            5,
            "this is a comment",
        )

        // Verify
        verify(
            postRequestedFor(urlEqualTo("/repos/my-owner/my-test-repo/issues/5/comments")).withRequestBody(
                equalToJson(
                    """
                    {
                      "body" : "this is a comment"
                    }
                    """,
                ),
            ),
        )
    }

    // TODO: write a test for the generate token func
}

package uk.ac.york.gpig.teamb.aiassistant.vcs.facades.github

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.testutils.mocking.MockGithubAPIOutput.mockGithubAPIBlob
import java.io.File

@AiAssistantTest
class GitHubFacadeTest {
    @SpykBean // spy instead of mock because we want some real methods to run
    private lateinit var sut: GitHubFacade

    @BeforeEach
    fun mockRepoAccess() {
        // mock github API output: this is basically an exact copy of an example response from the docs
        // page, but with the owner and repo name changed
        val getRepoOutput =
            File("src/test/resources/wiremock/github-api/get-repo-output.json").readText()
        stubFor(get("/repos/my-owner/my-test-repo").willReturn(ok().withBody(getRepoOutput)))
        every { sut.generateInstallationToken() } returns
            "my-fancy-token" // TODO think of a strategy to test the auth functionality. For now, we can
        // mock it away every time
    }

    @Nested
    @WireMockTest(httpPort = 3000)
    inner class PullRequestTests {
        @Test
        fun `creates a pull request with the right parameters`() {
            val createPROutput =
                File("src/test/resources/wiremock/github-api/create-pull-request-output.json").readText()
            // ^ same as above, except we do not care *at all* what the output is, we only need it for the
            // underlying github library to run without exceptions
            stubFor(
                post("/repos/my-owner/my-test-repo/pulls")
                    .willReturn(
                        ResponseDefinitionBuilder.responseDefinition().withStatus(201).withBody(createPROutput),
                    ),
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
                postRequestedFor(urlEqualTo("/repos/my-owner/my-test-repo/pulls"))
                    .withRequestBody(
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
    }

    @Nested
    @WireMockTest(httpPort = 3000)
    inner class CommentTests {
        @Test
        fun `writes a comment on the correct issue`() {
            // mock github API output: this is basically an exact copy of an example response from the
            // docs page, but with the owner and repo name changed
            val getIssueOutput =
                File("src/test/resources/wiremock/github-api/get-issue-output.json").readText()
            val createCommentOutput =
                File("src/test/resources/wiremock/github-api/create-comment-output.json").readText()
            // ^ same as above, except we do not care *at all* what the output is, we only need it for the
            // underlying github library to run without exceptions
            stubFor(
                get("/repos/my-owner/my-test-repo/issues/5").willReturn(ok().withBody(getIssueOutput)),
            )
            stubFor(
                post("/repos/my-owner/my-test-repo/issues/5/comments")
                    .willReturn(
                        ResponseDefinitionBuilder
                            .responseDefinition()
                            .withStatus(201)
                            .withBody(createCommentOutput),
                    ),
            )

            // Act
            sut.createComment("my-owner/my-test-repo", 5, "this is a comment")

            // Verify
            verify(
                postRequestedFor(urlEqualTo("/repos/my-owner/my-test-repo/issues/5/comments"))
                    .withRequestBody(
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
    }

    @Nested
    @WireMockTest(httpPort = 3000)
    inner class BlobFetchTests {
        @Test
        fun `fetches file blob for single file`() {
            val pathToFetch = "README.md" // repo-root/README.md
            stubFor(
                get("/repos/my-owner/my-test-repo/contents/$pathToFetch?ref=HEAD")
                    .willReturn(ok().withBody(mockGithubAPIBlob(pathToFetch, "Some important text here..."))),
            )

            val result = sut.retrieveBlobs("my-owner/my-test-repo", listOf(pathToFetch))

            verify(
                getRequestedFor(urlEqualTo("/repos/my-owner/my-test-repo/contents/$pathToFetch?ref=HEAD")),
            )

            expectThat(result)
                .hasSize(1)
                .get { this[0] }
                .and {
                    get { this.path }.isEqualTo(pathToFetch)
                    get { this.contents }.isEqualTo("Some important text here...")
                }
        }

        @Test
        fun `handles multiple files`() {
            val expectedFiles =
                mapOf(
                    "README.md" to
                        """
                    # I am a readme
                    ## Here is my cool sub heading
                """,
                    "code/main.py" to
                        """
                    def main():
                        print("I am the main function")
                """,
                    "code/utils/greet.py" to
                        """
                    def greet(name):
                        print(f"Hello {name}!")
                """,
                )
            expectedFiles.forEach { (filename, content) ->
                stubFor(
                    get("/repos/my-owner/my-test-repo/contents/$filename?ref=HEAD")
                        .willReturn(ok().withBody(mockGithubAPIBlob(filename, content))),
                )
            }

            val result = sut.retrieveBlobs("my-owner/my-test-repo", expectedFiles.keys.toList())

            expectedFiles.keys.forEach {
                verify(getRequestedFor(urlEqualTo("/repos/my-owner/my-test-repo/contents/$it?ref=HEAD")))
            }

            expectThat(result).hasSize(3).and {
                get { this.map { it.path } }
                    .containsExactly("README.md", "code/main.py", "code/utils/greet.py")
            }
        }
    }

    @Nested
    @WireMockTest(httpPort = 3000)
    inner class TreeFetchTests {
        @Test
        fun `fetches the flattened file tree`() {
            val getTreeOutput =
                File("src/test/resources/wiremock/github-api/get-repo-tree-output.json").readText()
            stubFor(
                get(urlMatching("/repos/my-owner/my-test-repo/git/trees/main.*"))
                    .willReturn(ok().withBody(getTreeOutput)),
            )
            val result = sut.fetchFileTree("my-owner/my-test-repo", "main")
            verify(
                getRequestedFor(urlMatching("/repos/my-owner/my-test-repo/git/trees/main.*"))
                    .withQueryParam("recursive", equalToJson("1")),
            )
            expectThat(result)
                .containsExactlyInAnyOrder(
                    "README.md",
                    "greeter/greeting.py",
                    "math/addition.py",
                    "math/subtraction.py",
                    "math/utils/is_three.py",
                )
        }
    }
}

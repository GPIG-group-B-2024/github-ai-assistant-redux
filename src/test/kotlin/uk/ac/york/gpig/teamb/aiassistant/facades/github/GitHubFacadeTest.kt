package uk.ac.york.gpig.teamb.aiassistant.facades.github

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
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File

@SpringBootTest
@WireMockTest(httpPort = 3000)
class GitHubFacadeTest {
    @Autowired
    private lateinit var sut: GitHubFacade

    @Test
    fun `smoke test`() {
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
            endpoint = "http://localhost:3000",
            // ^ replace the github API url with this, this is also where wiremock expects incoming requests
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
}

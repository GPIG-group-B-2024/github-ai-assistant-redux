package uk.ac.york.gpig.teamb.aiassistant.controllers

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.ac.york.gpig.teamb.aiassistant.services.git.GitService
import uk.ac.york.gpig.teamb.aiassistant.services.github.GitHubService
import uk.ac.york.gpig.teamb.aiassistant.utils.filesystem.withTempDir
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload

/**
 * Receives incoming webhook events.
 *
 * The exact event types are configured on GitHub, and currently only include updates to issues.
 * */
@RestController
class IssueCreationController(
    val gitService: GitService,
    val githubService: GitHubService,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/new-issue")
    fun receiveNewIssue(@RequestBody body: String) = withTempDir { tempDir ->
        val issueContents = Gson().fromJson(body, WebhookPayload::class.java)
        logger.info("Received a new issue!")
        val gitFile = gitService.cloneRepo(tempDir.toFile())
        val token = githubService.generateInstallationToken()
        gitService.createBranch(gitFile, "my-branch")
        gitService.commitTextFile(gitFile, "my-branch", "test.txt", "some code solving ${issueContents.issue.body} ")
        gitService.pushBranch(gitFile, "my-branch", token)
        githubService.createPullRequest("main", "my-branch", "My pull request", "My pull request body.\ncloses #${issueContents.issue.number}")
    }
}

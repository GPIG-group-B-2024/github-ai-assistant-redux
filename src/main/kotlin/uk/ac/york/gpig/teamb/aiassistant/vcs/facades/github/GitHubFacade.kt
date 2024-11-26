package uk.ac.york.gpig.teamb.aiassistant.vcs.facades.github

import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.york.gpig.teamb.aiassistant.utils.auth.JWTGenerator

/**
 * Interacts with the GitHub API for GitHub-specific things like pull requests, comments, issues etc.
 * */
@Service
class GitHubFacade {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * We use a different endpoint address for mocking github requests in testing.
     * In production, use the normal github endpoint
     * */
    @Value("\${app_settings.github_api_endpoint:https://api.github.com}")
    private lateinit var githubEndpoint: String

    fun createPullRequest(
        repoName: String,
        baseBranch: String = "main",
        featureBranch: String,
        title: String,
        body: String,
    ) {
        val repo = authenticateAndCheckoutRepo(repoName)
        val pullRequest = repo.createPullRequest(title, featureBranch, baseBranch, body)
        logger.info("Successfully created pull request ${pullRequest.number} in repository ${repo.name}")
    }

    fun createComment(
        repoName: String,
        issueNumber: Int,
        body: String,
    ) {
        val repo = authenticateAndCheckoutRepo(repoName)
        val issue = repo.getIssue(issueNumber)
        issue.comment(body)
        logger.info("Successfully commented on issue ${issue.number} in repository ${repo.name}")
    }

    /**
     * Generate an installation token for use with the wider GitHub API.
     *
     * */
    fun generateInstallationToken(): String =
        GitHubBuilder()
            .withJwtToken(JWTGenerator.generateJWT()) // use the private key to generate a *JWT*
            .build()
            .app
            .listInstallations()
            .first()
            .createToken() // authenticate with the JWT and generate an *Installation token*.
            .create() // These tokens are short-lived, so for now, create a new one for each action. TODO: look into caching
            .token

    /**
     * Helper function that generates a fresh installation token and uses it to get a github repository by the given name.
     * */
    internal fun authenticateAndCheckoutRepo(repoName: String): GHRepository {
        val token = generateInstallationToken()
        val github = GitHubBuilder().withEndpoint(githubEndpoint).withAppInstallationToken(token).build()
        return github.getRepository(repoName).also { logger.info("Successfully authenticated") }
    }
}

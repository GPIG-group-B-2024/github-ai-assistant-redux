package uk.ac.york.gpig.teamb.aiassistant.facades.github

import org.kohsuke.github.GitHubBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.ac.york.gpig.teamb.aiassistant.utils.auth.JWTGenerator

/**
 * Interacts with the GitHub API for GitHub-specific things like pull requests, comments, issues etc.
 * */
@Service
class GitHubFacade {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun createPullRequest(
        repoName: String,
        baseBranch: String = "main",
        featureBranch: String,
        title: String,
        body: String,
        /**The endpoint to use to connect to github. Should only be modified for tests*/
        endpoint: String = "https://api.github.com",
    ) {
        val token = generateInstallationToken()
        val github = GitHubBuilder().withEndpoint(endpoint).withAppInstallationToken(token).build()
        val repo = github.getRepository(repoName)
        logger.info("Successfully authenticated")
        val pullRequest = repo.createPullRequest(title, featureBranch, baseBranch, body)
        logger.info("Successfully created pull request ${pullRequest.number} in repository ${repo.name}")
    }

    fun createComment(
        repoName: String,
        issueId: Long,
        body: String,
    ) {
        // TODO: write a comment on an issue
        val token = generateInstallationToken()
        val github = GitHubBuilder().withAppInstallationToken(token).build()
        val repo = github.getRepository(repoName)
        val issue = repo.getIssue(issueId.toInt())
        logger.info("Successfully authenticated")
        val issueComment = issue.comment(body)
        logger.info("Successfully commented on issue ${issue.id} in repository ${repo.name}")
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
}

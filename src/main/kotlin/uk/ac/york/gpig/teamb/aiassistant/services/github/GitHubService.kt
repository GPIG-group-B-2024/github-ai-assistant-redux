package uk.ac.york.gpig.teamb.aiassistant.services.github

import org.kohsuke.github.GitHubBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.york.gpig.teamb.aiassistant.utils.auth.JWTGenerator

/**
 * Interacts with the GitHub API for GitHub-specific things like pull requests, comments, issues etc.
 * */
@Service
class GitHubService {
    val logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${target-repo.url}")
    lateinit var repoUrl: String

    @Value("\${target-repo.name}")
    lateinit var repoName: String

    fun createPullRequest(baseBranch: String = "main", featureBranch: String, title: String, body: String) {
        val token = generateInstallationToken()
        val github = GitHubBuilder().withAppInstallationToken(token).build()
        val repo = github.getRepository(repoName)
        repo.createPullRequest(title, featureBranch, baseBranch, body)
    }

    /**
     * Generate an installation token for use with the wider GitHub API.
     *
     * */
    fun generateInstallationToken(): String = GitHubBuilder()
        .withJwtToken(
            JWTGenerator.generateJWT(), // use the private key to generate a *JWT*
        )
        .build()
        .app
        .listInstallations()
        .first()
        .createToken() // authenticate with the JWT and generate an *Installation token*.
        .create() // These tokens are short-lived, so for now, create a new one for each action. TODO: look into caching
        .token
}

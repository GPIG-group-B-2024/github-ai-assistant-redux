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
class GitHubFacade(
    @Value("\${app_settings.github_app_key}")
    val githubKeyFileContents: String,
) {
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
    ) = withAuthInRepo(repoName) { repo ->
        val pullRequest = repo.createPullRequest(title, featureBranch, baseBranch, body)
        logger.info("Successfully created pull request ${pullRequest.number} in repository ${repo.name}")
    }

    fun createComment(
        repoName: String,
        issueNumber: Int,
        body: String,
    ) = withAuthInRepo(repoName) { repo ->
        val issue = repo.getIssue(issueNumber)
        issue.comment(body)
        logger.info("Successfully commented on issue ${issue.number} in repository ${repo.name}")
    }

    /**
     * Retrieve a list of "blobs" (raw file contents) for the provided list of paths (from repo root).
     *
     * Returns a list of filenames with their contents.
     * */
    fun retrieveBlobs(
        repoName: String,
        paths: List<String>,
        ref: String = "HEAD",
    ): List<FileBlob> =
        withAuthInRepo(repoName) { repo ->
            paths.map { path ->
                logger.info("Trying to read file $path of repo $repoName (ref: $ref)")
                // bit of a mouthful - basically, get the text from the bytes returned by the API
                val fileContents =
                    repo.getFileContent(path, ref) // make request...
                        .read() // get bytes...
                        .reader() // create reader...
                        .use { it.readText() } // get text and close the reader
                logger.info("Success")
                FileBlob(
                    path = path,
                    contents = fileContents,
                )
            }
        }

    /**
     * Fetches the __flattened__ file structure of the repo i.e. the path to every file.
     * */
    fun fetchFileTree(
        repoName: String,
        branchName: String = "main",
    ): List<String> =
        withAuthInRepo(repoName) { repo ->
            repo.getTreeRecursive(branchName, 1).tree.filter { it.type == "blob" }.map { it.path }
        }

    /**
     * Generate an installation token for use with the wider GitHub API.
     *
     * */
    fun generateInstallationToken(): String =
        GitHubBuilder()
            .withJwtToken(JWTGenerator.generateJWT(githubKeyFileContents)) // use the private key to generate a *JWT*
            .build()
            .app
            .listInstallations()
            .first()
            .createToken() // authenticate with the JWT and generate an *Installation token*.
            .create() // These tokens are short-lived, so for now, create a new one for each action. TODO: look into caching
            .token

    /**
     * Authenticate, checkout the repository with a given name, do some stuff with it and return the output (if any)
     *
     * @param block the function to run once the repo is checked out
     * */
    internal fun <R> withAuthInRepo(
        repoName: String,
        block: (GHRepository) -> R,
    ): R {
        val token = generateInstallationToken()
        val github = GitHubBuilder().withEndpoint(githubEndpoint).withAppInstallationToken(token).build()
        val repo = github.getRepository(repoName).also { logger.info("Successfully authenticated") }
        return block(repo)
    }
}

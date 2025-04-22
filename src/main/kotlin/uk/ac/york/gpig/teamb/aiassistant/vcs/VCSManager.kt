package uk.ac.york.gpig.teamb.aiassistant.vcs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.ac.york.gpig.teamb.aiassistant.database.c4.facades.C4NotationReadFacade
import uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas.LLMPullRequestData
import uk.ac.york.gpig.teamb.aiassistant.utils.filesystem.withTempDir
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload.Issue
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload.Repository
import uk.ac.york.gpig.teamb.aiassistant.vcs.entities.FileBlob
import uk.ac.york.gpig.teamb.aiassistant.vcs.facades.git.GitFacade
import uk.ac.york.gpig.teamb.aiassistant.vcs.facades.github.GitHubFacade

/** Manages the response to issues: interacts with the git repository and creates pull requests */
@Service
class VCSManager(
    val gitFacade: GitFacade,
    val gitHubFacade: GitHubFacade,
    val c4NotationReadFacade: C4NotationReadFacade,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    fun retrieveFileTree(
        repoName: String,
        branchName: String = "main",
    ): String = gitHubFacade.fetchFileTree(repoName, branchName).joinToString("\n")

    fun fetchFileBlobs(
        repoName: String,
        filePaths: List<String>,
    ): List<FileBlob> = gitHubFacade.retrieveBlobs(repoName, filePaths)

    fun processNewIssueComment(payload: WebhookPayload) {
        val (issue, _, repository, comment) = payload
        if (
            comment.user.login != "gpig-ai-assistant[bot]"
        ) { // TODO add login to config instead of hardcoding
            logger.info("Processing comment ${comment.id} on issue ${issue.number}")
            gitHubFacade.createComment(repository.fullName, issue.number, "This is a helpful comment")
            logger.info("Success!")
        } else {
            logger.info("Latest comment is from myself, aborting...")
        }
    }

    fun processChanges(
        repository: Repository,
        issue: Issue,
        changes: LLMPullRequestData,
    ) = withTempDir { tempDir ->
        logger.info("Processing issue ${issue.id}")
        val installationToken = gitHubFacade.generateInstallationToken()

        logger.info("Cloning git repo...")
        val gitFile =
            gitFacade.cloneRepo("${repository.url}.git", tempDir.toFile(), installationToken)

        logger.info("Creating a new branch linked to the issue...")
        val branchName = "${issue.number}-${issue.title.lowercase().replace(" ", "-")}"
        gitFacade.createBranch(gitFile, branchName)

        logger.info("Created branch $branchName, fetching file tree...")
        val fileTree = retrieveFileTree(repository.fullName, "main")

        logger.info("Fetched file tree, applying changes...")
        gitFacade.applyAndCommitChanges(gitFile, branchName, changes, fileTree, installationToken)

        logger.info("Changes commited, pushing to branch...")
        gitFacade.pushBranch(gitFile, branchName, installationToken)

        logger.info("Changes pushed, Creating Pull Request...")
        gitHubFacade.createPullRequest(
            repository.fullName,
            "main",
            branchName,
            changes.pullRequestTitle,
            changes.pullRequestBody,
        )

        logger.info("Success!")
    }
}

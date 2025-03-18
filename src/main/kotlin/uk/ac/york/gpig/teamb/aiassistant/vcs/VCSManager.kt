package uk.ac.york.gpig.teamb.aiassistant.vcs

import org.eclipse.jgit.api.Git
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.ac.york.gpig.teamb.aiassistant.database.c4.facades.C4NotationReadFacade
import uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas.LLMPullRequestData
import uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas.LLMPullRequestData.Change
import uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas.LLMPullRequestData.ChangeType
import uk.ac.york.gpig.teamb.aiassistant.utils.filesystem.withTempDir
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload.Issue
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload.Repository
import uk.ac.york.gpig.teamb.aiassistant.vcs.facades.git.GitFacade
import uk.ac.york.gpig.teamb.aiassistant.vcs.facades.github.GitHubFacade
import java.io.File

/**
 * Manages the response to issues: interacts with the git repository and creates pull requests
 * */
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

    fun processNewIssue(payload: WebhookPayload) =
        withTempDir { tempDir ->
            val (issue, _, repository, _) = payload
            logger.info("Processing issue ${issue.id}")
            val installationToken = gitHubFacade.generateInstallationToken()
            logger.info("Cloning git repo...")
            val gitFile = gitFacade.cloneRepo("${repository.url}.git", tempDir.toFile())
            logger.info("Creating a new branch linked to the issue...")
            val branchName = "${issue.number}-${issue.title.lowercase().replace(" ", "-")}"
            gitFacade.createBranch(gitFile, branchName)
            logger.info("Created branch $branchName, committing text file with issue body...")
            gitFacade.commitTextFile(
                gitFile,
                branchName,
                "file-from-issue-${issue.number}.txt",
                """
                Some code addressing the following problem:
                ${issue.body}
                """,
            )
            gitFacade.pushBranch(gitFile, branchName, installationToken)
            logger.info("Successfully pushed branch $branchName to upstream. Creating pull request...")
            gitHubFacade.createPullRequest(
                repository.fullName,
                "main",
                branchName,
                issue.title,
                "closes #${issue.number}\nThe body for pull request solving the following issue: ${issue.body}",
            ) // include the "closes" note - it is a "magic" word that links PR's to issues
            // https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue#linking-a-pull-request-to-an-issue-using-a-keyword
            logger.info("Success!")
        }

    fun processNewIssueComment(payload: WebhookPayload) {
        val (issue, _, repository, comment) = payload
        if (comment.user.login != "gpig-ai-assistant[bot]") { // TODO add login to config instead of hardcoding
            logger.info("Processing comment ${comment.id} on issue ${issue.number}")
            gitHubFacade.createComment(
                repository.fullName,
                issue.number,
                "This is a helpful comment",
            )
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
        val gitFile = gitFacade.cloneRepo("${repository.url}.git", tempDir.toFile(), installationToken)

        logger.info("Creating a new branch linked to the issue...")
        val branchName = "${issue.number}-${issue.title.lowercase().replace(" ", "-")}"
        gitFacade.createBranch(gitFile, branchName)

        logger.info("Created branch $branchName, fetching file tree...")
        val fileTree = gitHubFacade.fetchFileTree(repository.fullName, branchName)

        logger.info("Fetched file tree, applying changes...")
        val git = Git.open(gitFile)
        git.checkout().setName(branchName).call()
        for (change: Change in changes.updatedFiles) {
            when (change.type) {
                ChangeType.MODIFY -> {
                    if (!fileTree.contains(change.filePath)) {
                        // if file does not exist throw error
                        throw FileNotFoundException("Cannot modify '${change.filePath}' as it does not exist")
                    }
                    // update file
                    addFile(gitFile.parentFile, change.filePath, change.newContents)
                }
                ChangeType.CREATE -> {
                    if (fileTree.contains(change.filePath)) {
                        // if file exists throw error
                        throw FileAlreadyExistsException("Cannot create '${change.filePath}' as it already exists")
                    }
                    // add file
                    addFile(gitFile.parentFile, change.filePath, change.newContents)
                }
                ChangeType.DELETE -> {
                    if (!fileTree.contains(change.filePath)) {
                        // if file does not exist throw error
                        throw FileNotFoundException("Cannot delete '${change.filePath}' as it does not exist")
                    }
                    // remove file
                    git.rm().addFilepattern(change.filePath).call()
                }
            }
        }

        logger.info("Staging and commiting changes...")
        gitFacade.stageAndCommitChanges(git, changes.pullRequestTitle) // TODO: have the model produce an actual commit message?

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

    fun addFile(
        parentFile: File,
        filePath: String,
        contents: String,
    ): File {
        val newFile = File(parentFile, filePath)
        newFile.writeText(contents)
        return newFile
    }
}

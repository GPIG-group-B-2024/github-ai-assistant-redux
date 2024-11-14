package uk.ac.york.gpig.teamb.aiassistant.managers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.ac.york.gpig.teamb.aiassistant.facades.git.GitFacade
import uk.ac.york.gpig.teamb.aiassistant.facades.github.GitHubFacade
import uk.ac.york.gpig.teamb.aiassistant.utils.filesystem.withTempDir
import uk.ac.york.gpig.teamb.aiassistant.utils.types.WebhookPayload

/**
 * Manages the response to issues: interacts with the git repository and creates pull requests
 * */
@Service
class IssueManager(
    val gitFacade: GitFacade,
    val gitHubFacade: GitHubFacade,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    fun processNewIssue(payload: WebhookPayload) =
        withTempDir { tempDir ->
            val (issue, _, repository) = payload
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
        // TODO: reply on the issue with a comment
        val (issue, _, repository) = payload
        logger.info("Processing issue ${issue.id}")
        gitHubFacade.createComment(
            repository.fullName,
            issue.id,
            "This is a helpful comment",
        )
        logger.info("Success!")
    }
}

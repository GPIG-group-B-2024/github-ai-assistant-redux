package uk.ac.york.gpig.teamb.aiassistant.vcs.facades.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas.LLMPullRequestData
import uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas.LLMPullRequestData.Change
import uk.ac.york.gpig.teamb.aiassistant.llm.responseSchemas.LLMPullRequestData.ChangeType
import java.io.File

/**
 * Perform git (**Not GitHub**) activities, such as committing files, making branches and pushing to remotes.
 * */
@Service
class GitFacade {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val personIdent =
        PersonIdent(
            "gpig-ai-assistant[bot]",
            "187530873+gpig-ai-assistant@users.noreply.github.com",
            // ^ this is public and OK to hardcode *for now*
        )

    /**
     * Clone a repository into a given folder and return the path to the `.git` folder.
     *
     * Most of the git API uses the `.git` folder to identify the repository, therefore this function provides a convienient
     * way to obtain the path for future use.
     * */

    fun cloneRepo(
        repoUrl: String,
        clonePath: File,
        token: String,
    ): File {
        logger.info("Cloning repo at $repoUrl into $clonePath with auth token")
        Git.cloneRepository()
            .setURI(repoUrl)
            .setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", token))
            .setDirectory(clonePath)
            .call()
        val gitPath = File(clonePath, ".git")
        return gitPath
    }

    /**
     * Create a branch with given name from the latest `main` branch
     * */
    fun createBranch(
        gitPath: File,
        branchName: String,
    ) {
        logger.info("Creating local branch $branchName in $gitPath")
        Git.open(gitPath).branchCreate().setName(branchName).call()
        logger.info("Success!")
        logger.info("All branches are: ${Git.open(gitPath).branchList().call().map { it.name }}")
    }

    fun applyAndCommitChanges(
        gitFile: File,
        branchName: String,
        changes: LLMPullRequestData,
        fileTree: String,
        token: String,
    ) {
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
        stageAndCommitChanges(git, changes.pullRequestTitle, token) // TODO: have the model produce an actual commit message?
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

    fun stageAndCommitChanges(
        git: Git,
        commitMessage: String,
        token: String,
    ) {
        git.add().setUpdate(true).addFilepattern(".").call() // stage modified and deleted
        git.add().addFilepattern(".").call() // stage modified and new
        git.commit().setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", token)).setMessage(commitMessage).call()
    }

    /**
     * Checkout the branch with the provided name and push it to the upstream (github) location.
     * Requires the app **Installation token** (not the JWT), see github API docs.
     * */
    fun pushBranch(
        gitPath: File,
        branchName: String,
        token: String,
    ) {
        val repo = Git.open(gitPath)
        repo.checkout().setName(branchName).call()
        logger.info("Pushing branch $branchName to remote")
        repo.push().setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", token)).call()
    }
}

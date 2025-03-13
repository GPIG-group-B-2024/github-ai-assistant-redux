package uk.ac.york.gpig.teamb.aiassistant.vcs.facades.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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
    @Deprecated(message = "use the one with auth")
    fun cloneRepo(
        repoUrl: String,
        clonePath: File,
    ): File {
        logger.info("Cloning repo at $repoUrl into $clonePath")
        Git.cloneRepository().setURI(repoUrl).setDirectory(clonePath).call()
        val gitPath = File(clonePath, ".git")
        return gitPath
    }

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

    /**
     * This is for the initial prototype only and will most likely be removed:
     * Create a single `.txt` file with a given name and contents and commit it in a new branch with the provided name.
     * */
    fun commitTextFile(
        gitPath: File,
        branchName: String,
        name: String,
        content: String,
    ) {
        val repo = Git.open(gitPath)
        logger.info("Committing a text file called '$name' in branch $branchName in $gitPath")
        repo.checkout().setName(branchName).call()
        logger.info("Creating $name in ${gitPath.parentFile}")
        val newFile = File(gitPath.parentFile, name) // the new file to be committed
        newFile.writeText(content)
        logger.info("Adding file")
        repo.add().addFilepattern(".")
            .call() // Add everything not in .gitignore TODO see if more fine-grained pattern is needed
        logger.info("Committing file")
        repo.commit().setCommitter(personIdent).setAuthor(personIdent).setMessage("Added $name").call()
    }

    fun stageAndCommitChanges(
        git: Git,
        commitMessage: String,
    ) {
        git.add().setUpdate(true).addFilepattern(".").call() // stage modified and deleted
        git.add().addFilepattern(".").call() // stage modified and new
        git.commit().setMessage(commitMessage).call() 
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

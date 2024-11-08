package uk.ac.york.gpig.teamb.aiassistant.services.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

/**
 * Perform git (**Not GitHub**) activities, such as committing files, making branches and pushing to remotes.
 * */
@Service
class GitService {
    val logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${target-repo.url}")
    lateinit var repoUrl: String

    val personIdent = PersonIdent("gpig-ai-assistant[bot]", "187530873+gpip-ai-assistant@users.noreply.github.com")
    fun cloneRepo(clonePath: File): File {
        logger.info("Cloning repo at $repoUrl into $clonePath")
        Git.cloneRepository().setURI(repoUrl).setDirectory(clonePath).call()
        Git.shutdown()
        val gitPath = File(clonePath, ".git")
        return gitPath
    }

    fun createBranch(gitPath: File, branchName: String) {
        logger.info("Creating local branch $branchName in $gitPath")
        Git.open(gitPath).branchCreate().setName(branchName).call()
        logger.info("Success!")
        logger.info("All branches are: ${Git.open(gitPath).branchList().call().map{it.name}}")
    }

    fun commitTextFile(gitPath: File, branchName: String, name: String, content: String) {
        val repo = Git.open(gitPath)
        logger.info("Committing a text file called '$name' in branch $branchName in $gitPath")
        repo.checkout().setName(branchName).call()
        logger.info("Creating $name in ${gitPath.parentFile}")
        val file = File(gitPath.parentFile, name)
        file.writeText(content)
        logger.info("Adding file")
        repo.add().addFilepattern(".").call()
        logger.info("Committing file")
        repo.commit().setCommitter(personIdent).setAuthor(personIdent).setMessage("Added $name").call()
    }

    fun pushBranch(gitPath: File, branchName: String, token: String) {
        val repo = Git.open(gitPath)
        repo.checkout().setName(branchName).call()
        logger.info("Pushing branch $branchName to remote")
        repo.push().setCredentialsProvider(UsernamePasswordCredentialsProvider("x-access-token", token)).call()
    }
}

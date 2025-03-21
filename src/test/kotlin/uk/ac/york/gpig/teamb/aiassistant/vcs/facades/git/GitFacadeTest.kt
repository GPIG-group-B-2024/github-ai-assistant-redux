package uk.ac.york.gpig.teamb.aiassistant.vcs.facades.git

import com.github.sparsick.testcontainers.gitserver.GitServerVersions
import com.github.sparsick.testcontainers.gitserver.http.GitHttpServerContainer
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import strikt.api.expectDoesNotThrow
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.one
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.utils.filesystem.withTempDir
import java.io.File
import kotlin.io.path.listDirectoryEntries

@AiAssistantTest
class GitFacadeTest {
    @Autowired
    private lateinit var sut: GitFacade

    companion object {
        val gitServer =
            GitHttpServerContainer(GitServerVersions.V2_43.dockerImageName)

        @BeforeAll
        @JvmStatic
        fun startContainerAndSetupMainBranch() =
            withTempDir<Unit> { tempDir ->
                gitServer.start()
                // make an empty commit so that the repository has a remote master branch
                Git.cloneRepository().setURI(gitServer.gitRepoURIAsHttp.toString()).setDirectory(tempDir.toFile()).call()
                val gitFile = File(tempDir.toFile(), ".git")
                Git.open(gitFile).commit().setMessage("Initial commit").call()
                Git.open(gitFile).push().call()
            }

        @AfterAll
        @JvmStatic
        fun stopContainer() {
            gitServer.stop()
        }
    }

    @Test
    fun `can clone existing repository`() =
        withTempDir<Unit> { tempDir ->
            expectDoesNotThrow { sut.cloneRepo(gitServer.gitRepoURIAsHttp.toString(), tempDir.toFile(), "super-secret-token") }
            // the repository is currently empty: check that the .git file is present (meaning the git clone was successful)
            val gitFile = tempDir.listDirectoryEntries().find { it.fileName.toString().contains(".git") }
            expectThat(gitFile).isNotNull()
        }

    @Test
    fun `creates new local branch`() =
        withTempDir<Unit> { tempDir ->
            // setup
            // clone repo using the 3rd party tool to not rely on our own implementation
            Git.cloneRepository().setURI(gitServer.gitRepoURIAsHttp.toString()).setDirectory(tempDir.toFile()).call()
            val gitPath = File(tempDir.toFile(), ".git")
            Git.open(gitPath).commit().setMessage("initial commit").call() // create an initial commit to establish a HEAD
            // act
            sut.createBranch(gitPath, "new-branch")
            // verify (again, using the 3rd party tool to list branches)
            val branchNames = Git.open(gitPath).branchList().call().map { it.name }
            expectThat(branchNames).one {
                this.contains("new-branch") // a single branch contains the expected branch name
            }
        }

    @Test
    fun `pushes branch to remote`() =
        withTempDir<Unit> { tempDir ->
            // setup
            // clone repo using the 3rd party tool to not rely on our own implementation
            println(Git.lsRemoteRepository().setRemote(gitServer.gitRepoURIAsHttp.toString()).call())
            Git.cloneRepository().setURI(gitServer.gitRepoURIAsHttp.toString()).setDirectory(tempDir.toFile()).call()
            val gitPath = File(tempDir.toFile(), ".git")
            Git.open(gitPath).commit().setMessage("initial commit").call() // create an initial commit to establish a HEAD
            Git.open(gitPath).branchCreate().setName("new-branch").call() // create a new branch (which we will push)
            // act
            sut.pushBranch(
                gitPath,
                "new-branch",
                "super-secret-token",
            ) // NOTE: we don't need an actual token since the testcontainer doesnt require auth

            // verify
            // use the 3rd party tool to look at the remote repo (in the testcontainer) and make sure the expected branch is there
            val remoteBranches =
                Git.lsRemoteRepository().setRemote(gitServer.gitRepoURIAsHttp.toString()).call().map { it.name }

            expectThat(remoteBranches).one {
                this.contains(
                    "new-branch",
                )
            }
        }

    @Test
    fun `creates a new file with correct data`() {
        val contents = "some contents"
        withTempDir { tempDir ->

            // Act
            val myFile = sut.addFile(tempDir.toFile(), "newfile.txt", contents)
            // Verify
            expectThat(myFile.readText()).isEqualTo(contents)
        }
    }

    @Test
    fun `overwrites data of an existing file`() {
        val contents = "some contents"
        val filePath = "newFile.txt"
        withTempDir { tempDir ->
            val newFile = File(tempDir.toFile(), filePath)
            newFile.writeText("This will be overwritten")
            // Act
            val myFile = sut.addFile(tempDir.toFile(), filePath, contents)
            // Verify
            expectThat(myFile.readText()).isEqualTo(contents)
        }
    }
}

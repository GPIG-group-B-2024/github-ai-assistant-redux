package uk.ac.york.gpig.teamb.aiassistant.facades

import com.github.sparsick.testcontainers.gitserver.GitServerVersions
import com.github.sparsick.testcontainers.gitserver.http.GitHttpServerContainer
import com.ninjasquad.springmockk.SpykBean
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import strikt.api.expectDoesNotThrow
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isNotNull
import strikt.assertions.one
import uk.ac.york.gpig.teamb.aiassistant.facades.git.GitFacade
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.listDirectoryEntries

@SpringBootTest
class GitFacadeTest {
    @SpykBean
    private lateinit var sut: GitFacade

    companion object {
        val gitServer =
            GitHttpServerContainer(GitServerVersions.V2_43.getDockerImageName())

        @BeforeAll
        @JvmStatic
        fun startContainerAndSetupMainBranch() {
            val tempDir = createTempDirectory()
            gitServer.start()
            // make an empty commit so that the repository has a remote master branch
            Git.cloneRepository().setURI(gitServer.gitRepoURIAsHttp.toString()).setDirectory(tempDir.toFile()).call()
            val gitFile = File(tempDir.toFile(), ".git")
            Git.open(gitFile).commit().setMessage("Initial commit").call()
            Git.open(gitFile).push().call()
            tempDir.toFile().deleteRecursively()
        }

        @AfterAll
        @JvmStatic
        fun stopContainer() {
            gitServer.stop()
        }
    }

    @Test
    fun `can clone existing repository`() {
        val tempDir = createTempDirectory()
        expectDoesNotThrow { sut.cloneRepo(gitServer.gitRepoURIAsHttp.toString(), tempDir.toFile()) }
        // the repository is currently empty: check that the .git file is present (meaning the git clone was successful)
        val gitFile = tempDir.listDirectoryEntries().find { it.fileName.toString().contains(".git") }
        expectThat(gitFile).isNotNull()
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `creates new local branch`() {
        val tempDir = createTempDirectory()
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
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `pushes branch to remote`() {
        val tempDir = createTempDirectory()
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
        val remoteBranches = Git.lsRemoteRepository().setRemote(gitServer.gitRepoURIAsHttp.toString()).call().map { it.name }

        expectThat(remoteBranches).one {
            this.contains(
                "new-branch",
            )
        }
        tempDir.toFile().deleteRecursively()
    }
}

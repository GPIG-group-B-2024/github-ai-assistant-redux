package uk.ac.york.gpig.teamb.aiassistant.database.facades

import org.jooq.DSLContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.GitRepoBuilder.Companion.gitRepo
import java.util.UUID

@AiAssistantTest
class C4NotationReadFacadeTest {
    @Autowired
    private lateinit var sut: C4NotationReadFacade

    @Autowired
    private lateinit var ctx: DSLContext

    @Nested
    @DisplayName("checkRepositoryExists()")
    inner class CheckRepositoryTests {
        @Test
        fun `returns false when repository does not exist`() {
            val unknownRepoName = "some-dev/unknown-repo"
            expectThat(sut.checkRepositoryExists(unknownRepoName)).isFalse()
        }
    }

    @Nested
    @DisplayName("getRepositoryWorkspace()")
    inner class GetRepositoryWorkspaceTest {
        @Test
        fun `returns the repository workspace entity`() {
            val workspaceId = UUID.randomUUID()
            val repoName = "my-dev/my-fancy-repo"
            val workspaceDescription = "a fancy repo doing a lot of fancy stuff"
            val workspaceName = "my-fancy-workspace"
            val repository =
                gitRepo {
                    this.fullName = repoName
                    this.workspace {
                        this.id = workspaceId
                        this.name = workspaceName
                        this.description = workspaceDescription
                    }
                }.create(ctx)

            val result = sut.getRepositoryWorkspace(repoName)

            expectThat(result).isNotNull().and {
                get { this.id }.isEqualTo(workspaceId)
                get { this.description }.isEqualTo(workspaceDescription)
                get { this.name }.isEqualTo(workspaceName)
            }
        }

        @Test
        fun `returns null when repository does not exist`() {
            val repoName = "my-fancy-repo"
            val repository =
                gitRepo {
                    this.fullName = "some-other-repo"
                }.create(ctx) // create a repo that should *NOT* be returned

            val result = sut.getRepositoryWorkspace(repoName)

            expectThat(result).isNull()
        }
    }
}

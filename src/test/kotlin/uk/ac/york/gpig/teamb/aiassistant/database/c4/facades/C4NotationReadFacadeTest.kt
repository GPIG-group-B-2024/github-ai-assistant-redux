package uk.ac.york.gpig.teamb.aiassistant.database.c4.facades

import org.jooq.DSLContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import uk.ac.york.gpig.teamb.aiassistant.enums.MemberType
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.GitRepoBuilder.Companion.gitRepo
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.MemberBuilder.Companion.member
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.RelationshipBuilder.Companion.relationship
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.WorkspaceBuilder.Companion.workspace
import java.util.UUID

@AiAssistantTest
class C4NotationReadFacadeTest {
    @Autowired private lateinit var sut: C4NotationReadFacade

    @Autowired private lateinit var ctx: DSLContext

    @Nested
    @DisplayName("checkRepositoryExists()")
    inner class CheckRepositoryTests {
        @Test
        fun `returns false when repository with given name does not exist`() {
            val unknownRepoName = "some-dev/unknown-repo"
            expectThat(sut.checkRepositoryExists(unknownRepoName)).isFalse()
        }

        @Test
        fun `returns true when repository with given name exists`() {
            val repoName = "my-cool-repo"
            gitRepo { this.fullName = repoName }.create(ctx)

            expectThat(sut.checkRepositoryExists(repoName)).isTrue()
        }

        @Test
        fun `returns false when repository with given ID does not exist`() {
            val unknownRepoId = UUID.randomUUID()
            expectThat(sut.checkRepositoryExists(unknownRepoId)).isFalse()
        }

        @Test
        fun `returns true when repository with given ID exists`() {
            val repoId = UUID.randomUUID()
            gitRepo { this.id = repoId }.create(ctx)

            expectThat(sut.checkRepositoryExists(repoId)).isTrue()
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
                gitRepo { this.fullName = "some-other-repo" }
                    .create(ctx) // create a repo that should *NOT* be returned

            val result = sut.getRepositoryWorkspace(repoName)

            expectThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("getMembers()")
    inner class GetMembersTests {
        @Test
        fun `returns the members for a given workspace`() {
            val workspaceId = UUID.randomUUID()
            member {
                this.workspace { this.id = workspaceId }
                this.name = "my-fancy-component"
                this.type = MemberType.COMPONENT
            }.create(ctx)

            member {
                this.workspace { this.id = workspaceId }
                this.name = "my-fancy-user"
                this.type = MemberType.PERSON
            }.create(ctx)

            member {
                this.workspace { this.id = workspaceId }
                this.name = "my-fancy-container"
                this.type = MemberType.CONTAINER
            }.create(ctx)

            val memberNames = sut.getMembers(workspaceId).map { it.name }

            expectThat(memberNames)
                .containsExactlyInAnyOrder("my-fancy-component", "my-fancy-user", "my-fancy-container")
        }

        @Test
        fun `returns empty list when no members exist`() {
            val workspaceId = UUID.randomUUID()
            val otherWorkspaceId = UUID.randomUUID()
            workspace { this.id = workspaceId }.create(ctx)
            workspace { this.id = otherWorkspaceId }.create(ctx)

            member { this.workspace { this.id = otherWorkspaceId } }
                .create(ctx) // check that this is NOT returned

            val result = sut.getMembers(workspaceId)
            expectThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getRelationships()")
    inner class GetRelationshipsTests {
        @Test
        fun `returns the relationships for a given workspace`() {
            val workspaceId = UUID.randomUUID()
            val expectedStartId = UUID.randomUUID()
            val expectedEndId = UUID.randomUUID()
            relationship {
                this.workspace { this.id = workspaceId }
                this.startMember {
                    this.id = expectedStartId
                    this.workspace { this.id = workspaceId }
                }
                this.endMember {
                    this.id = expectedEndId
                    this.workspace { this.id = workspaceId }
                }
                this.description = "some cool relationship"
            }.create(ctx)

            val result = sut.getRelationships(workspaceId)
            expectThat(result).hasSize(1).and {
                get { this[0].from }.isEqualTo(expectedStartId)
                get { this[0].to }.isEqualTo(expectedEndId)
                get { this[0].description }.isEqualTo("some cool relationship")
            }
        }
    }

    @Nested
    @DisplayName("get ... byRepoName()")
    inner class GetXByRepoNameTests {
        @Test
        fun `gets repo ID by name`() {
            val repoName = "my-cool-repo"
            val repoId = UUID.randomUUID()
            val otherRepoId = UUID.randomUUID()
            gitRepo {
                this.fullName = repoName
                this.id = repoId
            }.create(ctx)
            gitRepo {
                this.fullName = "my-other-repo"
                this.id = otherRepoId
            }.create(ctx) // check this one isn't returned

            expectThat(sut.getRepoId(repoName)).isEqualTo(repoId)
        }

        @Test
        fun `gets repo URL by name`() {
            val repoName = "my-cool-repo"
            val repoUrl = "my-fancy-url"
            gitRepo {
                this.fullName = repoName
                this.url = repoUrl
            }.create(ctx)
            gitRepo { this.fullName = "my-other-repo" }.create(ctx) // check this one isn't returned

            expectThat(sut.getRepoUrl(repoName)).isEqualTo(repoUrl)
        }

        @Test
        fun `returns null if URL not found`() {
            val repoName = "my-cool-repo"
            val repoUrl = "my-fancy-url"
            gitRepo {
                this.fullName = repoName
                this.url = repoUrl
            }.create(ctx)

            expectThat(sut.getRepoUrl("unknown-repo")).isNull()
        }

        @Test
        fun `returns null if ID not found`() {
            val repoName = "my-cool-repo"
            val repoId = UUID.randomUUID()
            gitRepo {
                this.fullName = repoName
                this.id = repoId
            }.create(ctx)

            expectThat(sut.getRepoUrl("unknown-repo")).isNull()
        }
    }
}

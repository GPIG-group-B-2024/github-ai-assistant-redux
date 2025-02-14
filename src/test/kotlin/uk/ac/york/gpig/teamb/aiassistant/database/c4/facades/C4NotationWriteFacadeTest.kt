package uk.ac.york.gpig.teamb.aiassistant.database.c4.facades

import org.jooq.DSLContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4ElementEntity
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4RelationshipEntity
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4WorkspaceEntity
import uk.ac.york.gpig.teamb.aiassistant.enums.MemberType
import uk.ac.york.gpig.teamb.aiassistant.tables.references.GITHUB_REPOSITORY
import uk.ac.york.gpig.teamb.aiassistant.tables.references.MEMBER
import uk.ac.york.gpig.teamb.aiassistant.tables.references.RELATIONSHIP
import uk.ac.york.gpig.teamb.aiassistant.tables.references.WORKSPACE
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.GitRepoBuilder.Companion.gitRepo
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.MemberBuilder.Companion.member
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.WorkspaceBuilder.Companion.workspace
import java.util.UUID

@AiAssistantTest
class C4NotationWriteFacadeTest {
    @Autowired
    private lateinit var sut: C4NotationWriteFacade

    @Autowired
    private lateinit var ctx: DSLContext

    @Nested
    @DisplayName("linkRepoToWorkspace()")
    inner class LinkRepoToWorkspaceTest {
        @Test
        fun `can link github repository to C4 workspace`() {
            val repoName = "fancy-programmer/my-fancy-repo"
            val workspaceId = UUID.randomUUID()
            gitRepo(createWorkspace = false) {
                this.fullName = repoName
            }.create(ctx)

            workspace {
                this.id = workspaceId
            }.create(ctx)

            sut.linkRepoToWorkspace(repoName, workspaceId)

            expectThat(ctx.selectFrom(GITHUB_REPOSITORY).fetch().single()).isNotNull().and {
                get { this.workspaceId }.isEqualTo(workspaceId)
            }
        }
    }

    @Nested
    @DisplayName("writeMemberList()")
    inner class WriteMemberListTest {
        @Test
        fun `can write single C4 element`() {
            val workspaceId = UUID.randomUUID()
            workspace {
                this.id = workspaceId
            }.create(ctx)

            val element =
                C4ElementEntity(
                    id = UUID.randomUUID(),
                    parentId = null,
                    type = MemberType.COMPONENT,
                    name = "my-fancy-component",
                    description = "my fancy description",
                    workspaceId = workspaceId,
                )

            sut.writeMemberList(listOf(element))

            expectThat(ctx.selectFrom(MEMBER).fetch()).hasSize(1).and {
                get { this[0].id }.isEqualTo(element.id)
                get { this[0].parent }.isEqualTo(element.parentId)
                get { this[0].type }.isEqualTo(element.type)
                get { this[0].name }.isEqualTo(element.name)
                get { this[0].description }.isEqualTo(element.description)
                get { this[0].workspaceId }.isEqualTo(element.workspaceId)
            }
        }

        @Test
        fun `can write multiple C4 elements`() {
            val workspaceId = UUID.randomUUID()
            workspace {
                this.id = workspaceId
            }.create(ctx)

            val elements =
                (0..<10).map {
                    C4ElementEntity(
                        id = UUID.randomUUID(),
                        parentId = null,
                        type = MemberType.entries.random(),
                        name = "component-$it",
                        description = "Fancy description of component number $it",
                        workspaceId = workspaceId,
                    )
                }

            sut.writeMemberList(elements)

            expectThat(ctx.selectFrom(MEMBER).fetch()).hasSize(10).and {
                get { this.map { it.name } }.containsExactly((0..<10).map { "component-$it" })
            }
        }
    }

    @Nested
    @DisplayName("writeRelationshipsList()")
    inner class WriteRelationshipsListTest {
        @Test
        fun `can write single relationship`() {
            val workspaceId = UUID.randomUUID()
            workspace { this.id = workspaceId }.create(ctx)
            val fromMemberId = UUID.randomUUID()
            val toMemberId = UUID.randomUUID()
            member {
                this.id = fromMemberId
                this.name = "Controller"
                this.workspace { this.id = workspaceId }
            }.create(ctx)
            member {
                this.id = toMemberId
                this.name = "Service"
                this.workspace { this.id = workspaceId }
            }.create(ctx)

            val relationshipEntity =
                C4RelationshipEntity(
                    from = fromMemberId,
                    to = toMemberId,
                    fromName = "Controller",
                    toName = "Service",
                    description = "Calls methods and formats outputs",
                    workspaceId = workspaceId,
                )

            sut.writeRelationshipsList(listOf(relationshipEntity))

            expectThat(ctx.selectFrom(RELATIONSHIP).fetch().single()).isNotNull().and {
                get { this.workspaceId }.isEqualTo(workspaceId)
                get { this.description }.isEqualTo("Calls methods and formats outputs")
                get { this.startMember }.isEqualTo(fromMemberId)
                get { this.endMember }.isEqualTo(toMemberId)
            }
        }

        @Test
        fun `can write multiple relationships`() {
            val workspaceId = UUID.randomUUID()
            workspace { this.id = workspaceId }.create(ctx)
            val fromMemberIds = (0..<10).map { UUID.randomUUID() }
            val toMemberIds = (0..<10).map { UUID.randomUUID() }

            listOf(*fromMemberIds.toTypedArray(), *toMemberIds.toTypedArray()).forEach {
                member {
                    this.id = it
                    this.workspace { this.id = workspaceId }
                }.create(ctx)
            } // for every ID, create a member entity

            val relationshipEntities =
                fromMemberIds.zip(toMemberIds).map {
                        (from, to) ->
                    C4RelationshipEntity(
                        from = from,
                        to = to,
                        fromName = "some component",
                        toName = "some other component",
                        description = null,
                        workspaceId = workspaceId,
                    )
                } // create 10 pairs of IDs and create relationship entities

            sut.writeRelationshipsList(relationshipEntities)
            // check all relationships were created and the correct source and destination ID's are written to db
            expectThat(
                ctx.selectFrom(RELATIONSHIP).fetch().map {
                    it.startMember to it.endMember
                },
            ).hasSize(10)
                .containsExactlyInAnyOrder(fromMemberIds.zip(toMemberIds))
        }
    }

    @Nested
    @DisplayName("writeWorkspace()")
    inner class WriteWorkspaceTest {
        @Test
        fun `writes workspace entity to database`() {
            val workspaceEntity =
                C4WorkspaceEntity(
                    id = UUID.randomUUID(),
                    name = "My fancy workspace",
                    description = "A super fancy workspace with lots of components",
                )

            sut.writeWorkspace(workspaceEntity)

            expectThat(ctx.selectFrom(WORKSPACE).fetch()).hasSize(1).and {
                get { this[0].id }.isEqualTo(workspaceEntity.id)
                get { this[0].description }.isEqualTo(workspaceEntity.description)
                get { this[0].name }.isEqualTo(workspaceEntity.name)
            }
        }
    }
}

package uk.ac.york.gpig.teamb.aiassistant.database

import org.jooq.DSLContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import uk.ac.york.gpig.teamb.aiassistant.enums.MemberType
import uk.ac.york.gpig.teamb.aiassistant.tables.references.GITHUB_REPOSITORY
import uk.ac.york.gpig.teamb.aiassistant.tables.references.MEMBER
import uk.ac.york.gpig.teamb.aiassistant.tables.references.RELATIONSHIP
import uk.ac.york.gpig.teamb.aiassistant.tables.references.WORKSPACE
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.GitRepoBuilder.Companion.gitRepo
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.MemberBuilder.Companion.member
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.RelationshipBuilder.Companion.relationship
import uk.ac.york.gpig.teamb.aiassistant.testutils.toTriple
import java.util.UUID

@AiAssistantTest
class C4ManagerTest {
    @Autowired
    private lateinit var sut: C4Manager

    @Autowired
    private lateinit var ctx: DSLContext

    @Nested
    @DisplayName("Tests for retrieving a C4 model from the database")
    inner class RetrieveC4ModelTest {
        @Test
        fun smokeTest() {
            val workspaceId = UUID.randomUUID()
            val repoName = "my-fancy-repo"
            val parentId = UUID.randomUUID()
            val repoComponentId = UUID.randomUUID()
            val controllerComponentId = UUID.randomUUID()
            val loggerComponentId = UUID.randomUUID()

            gitRepo {
                this.fullName = repoName
                this.workspace {
                    this.id = workspaceId
                }
            }.create(ctx)

            member {
                this.id = parentId
                this.workspace { this.id = workspaceId }
                this.name = "my-software-system"
            }.create(ctx)

            relationship {
                this.workspace {
                    this.id = workspaceId
                }
                this.startMember {
                    this.name = "my-controller"
                    this.id = controllerComponentId
                    this.workspace { this.id = workspaceId }
                    this.parentId = parentId
                    this.description = "handles HTTP requests"
                }
                this.endMember {
                    this.name = "my-repository"
                    this.id = repoComponentId
                    this.workspace { this.id = workspaceId }
                    this.parentId = parentId
                    this.description = "handles database access"
                }
                this.description = "some cool relationship"
            }.create(ctx)

            member {
                this.id = loggerComponentId
                this.name = "logger"
                this.workspace { this.id = workspaceId }
                this.parentId = repoComponentId
                this.description = "prints error traces to stderr"
            }.create(ctx)

            expectThat(sut.gitRepoToStructurizrDsl(repoName)).isEqualTo(
                """
              |workspace "My fancy workspace" "My fancy description. Very cool!"{
              |   model {
              |       my-software-system = component "my-software-system" "handles incoming HTTP requests"{
              |		my-controller = component "my-controller" "handles HTTP requests"
              |		my-repository = component "my-repository" "handles database access"{
              |			logger = component "logger" "prints error traces to stderr"
              |			}
              |		}
              |       my-controller -> my-repository "some cool relationship"
              |   }
              |}
                """.trimMargin(),
            )
        }
    }

    @Nested
    @DisplayName("Tests for consuming a C4 model and storing it in the database")
    inner class ConsumeC4ModelTest {
        @Test
        fun `smoke test`() {
            // setup: create a git repo with no workspace
            gitRepo(createWorkspace = false) {
                this.fullName = "some-dev/my-weather-app"
            }.create(ctx)
            // a valid structurizr string (components and relationships only, no style or view info)
            val rawStructurizr =
                """
                workspace "My-weather-app" "A simple yet powerful weather app with a database and an HTTP controller that returns cool JSON data."{
                !impliedRelationships false
                model {
                    u = person "User"
                    ss = softwareSystem "Software System" {
                        wa = container "Web Application" {
                            cont = component "Controllers"
                            dba = component "Database Access"
                            serviceLayer = component "Business logic"
                        }
                        db = container "Database Schema" {
                            tags "Database"
                        }
                    }
                    wa -> db "Reads from and writes to"
                    serviceLayer -> dba "Converts raw database output to easy-to-read JSON"
                    cont -> u "Send and receive HTTP traffic"
                    cont -> serviceLayer "Calls functions corresponding to user requests"
                    dba -> db "Compiles and executes queries"

                }
                }
                """.trimIndent()

            // act
            sut.consumeStructurizrWorkspace(
                "some-dev/my-weather-app",
                rawStructurizr,
            )

            // verify
            // check workspace has been created
            val createdWorkspace = ctx.selectFrom(WORKSPACE).fetch()
            expectThat(createdWorkspace).hasSize(1).and {
                get { this[0].name }.isEqualTo("My-weather-app")
                get {
                    this[0].description
                }.isEqualTo("A simple yet powerful weather app with a database and an HTTP controller that returns cool JSON data.")
            }

            // check github repo has been linked to workspace
            val workspaceId = createdWorkspace.first().id

            expectThat(
                ctx.fetchExists(GITHUB_REPOSITORY.where(GITHUB_REPOSITORY.WORKSPACE_ID.eq(workspaceId))),
            ).isTrue()

            val createdComponents =
                ctx.select(MEMBER.ID, MEMBER.NAME, MEMBER.TYPE).from(MEMBER).fetch {
                    it.get(MEMBER.NAME)!! to it.get(MEMBER.TYPE)!!
                } // get a list of all created components, their names and types
            // check that all components are present
            expectThat(createdComponents).hasSize(7).containsExactlyInAnyOrder(
                "User" to MemberType.PERSON,
                "Software System" to MemberType.SOFTWARE_SYSTEM,
                "Database Schema" to MemberType.CONTAINER,
                "Web Application" to MemberType.CONTAINER,
                "Controllers" to MemberType.COMPONENT,
                "Business logic" to MemberType.COMPONENT,
                "Database Access" to MemberType.COMPONENT,
            )
            val endMemberAlias = MEMBER.`as`("end_member")
            // for each relationship, get the names of the start and end components as well as the relationship description
            val createdRelationships =
                ctx.select()
                    .from(RELATIONSHIP)
                    .join(MEMBER)
                    .on(MEMBER.ID.eq(RELATIONSHIP.START_MEMBER))
                    .join(endMemberAlias)
                    .on(endMemberAlias.ID.eq(RELATIONSHIP.END_MEMBER))
                    .fetch { Triple(it.get(MEMBER.NAME)!!, it.get(endMemberAlias.NAME)!!, it.get(RELATIONSHIP.DESCRIPTION)) }

            expectThat(createdRelationships).hasSize(5).containsExactlyInAnyOrder(
                "Web Application" to "Database Schema" toTriple "Reads from and writes to",
                "Business logic" to "Database Access" toTriple "Converts raw database output to easy-to-read JSON",
                "Controllers" to "User" toTriple "Send and receive HTTP traffic",
                "Controllers" to "Business logic" toTriple "Calls functions corresponding to user requests",
                "Database Access" to "Database Schema" toTriple "Compiles and executes queries",
            )
        }
    }
}

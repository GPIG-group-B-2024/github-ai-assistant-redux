package uk.ac.york.gpig.teamb.aiassistant.database.c4

import com.structurizr.dsl.StructurizrDslParserException
import org.jooq.DSLContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import strikt.api.expect
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.doesNotContain
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import strikt.assertions.none
import strikt.assertions.withNotNull
import uk.ac.york.gpig.teamb.aiassistant.database.exceptions.NotFoundException.NotFoundByNameException
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
    @Autowired private lateinit var sut: C4Manager

    @Autowired private lateinit var ctx: DSLContext

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
                this.workspace { this.id = workspaceId }
            }.create(ctx)

            member {
                this.id = parentId
                this.workspace { this.id = workspaceId }
                this.name = "my-software-system"
            }.create(ctx)

            relationship {
                this.workspace { this.id = workspaceId }
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

            expectThat(sut.gitRepoToStructurizrDsl(repoName))
                .isEqualTo(
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
            gitRepo(createWorkspace = false) { this.fullName = "some-dev/my-weather-app" }.create(ctx)
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
            sut.consumeStructurizrWorkspace("some-dev/my-weather-app", rawStructurizr)

            // verify
            // check workspace has been created
            val createdWorkspace = ctx.selectFrom(WORKSPACE).fetch()
            expectThat(createdWorkspace).hasSize(1).and {
                get { this[0].name }.isEqualTo("My-weather-app")
                get { this[0].description }
                    .isEqualTo(
                        "A simple yet powerful weather app with a database and an HTTP controller that returns cool JSON data.",
                    )
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
            expectThat(createdComponents)
                .hasSize(7)
                .containsExactlyInAnyOrder(
                    "User" to MemberType.PERSON,
                    "Software System" to MemberType.SOFTWARE_SYSTEM,
                    "Database Schema" to MemberType.CONTAINER,
                    "Web Application" to MemberType.CONTAINER,
                    "Controllers" to MemberType.COMPONENT,
                    "Business logic" to MemberType.COMPONENT,
                    "Database Access" to MemberType.COMPONENT,
                )
            val endMemberAlias = MEMBER.`as`("end_member")
            // for each relationship, get the names of the start and end components as well as the
            // relationship description
            val createdRelationships =
                ctx
                    .select()
                    .from(RELATIONSHIP)
                    .join(MEMBER)
                    .on(MEMBER.ID.eq(RELATIONSHIP.START_MEMBER))
                    .join(endMemberAlias)
                    .on(endMemberAlias.ID.eq(RELATIONSHIP.END_MEMBER))
                    .fetch {
                        Triple(
                            it.get(MEMBER.NAME)!!,
                            it.get(endMemberAlias.NAME)!!,
                            it.get(RELATIONSHIP.DESCRIPTION),
                        )
                    }

            expectThat(createdRelationships)
                .hasSize(5)
                .containsExactlyInAnyOrder(
                    "Web Application" to "Database Schema" toTriple "Reads from and writes to",
                    "Business logic" to
                        "Database Access" toTriple
                        "Converts raw database output to easy-to-read JSON",
                    "Controllers" to "User" toTriple "Send and receive HTTP traffic",
                    "Controllers" to
                        "Business logic" toTriple
                        "Calls functions corresponding to user requests",
                    "Database Access" to "Database Schema" toTriple "Compiles and executes queries",
                )
        }

        @Test
        fun `throws for unknown github repository`() {
            gitRepo(createWorkspace = false) { this.fullName = "some-dev/my-weather-app" }.create(ctx)

            expectThrows<NotFoundByNameException> {
                sut.consumeStructurizrWorkspace(
                    "unknown-repo",
                    """
                    workspace "blank" "should not get parsed - but is still valid structurizr" {
                        
                    }
                    """,
                )
            }.and {
                get { this.message }
                    .isEqualTo("Could not find github repository with name \"unknown-repo\"")
            }
        }
    }

    @Nested
    @DisplayName("Tests for initialising a github repo with a structurizr workspace")
    inner class InitialiseRepoTest {
        @Test
        fun `smoke test`() {
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
            val repoName = "some-coder/my-weather-app"
            val repoUrl = "https://github.com/some-coder/my-weather-app"

            sut.initializeWorkspace(repoName, repoUrl, rawStructurizr)

            // Step 1: Check repo was created
            // 1.1: Check there is one repo
            expectThat(ctx.fetchCount(GITHUB_REPOSITORY)).isEqualTo(1)
            // 1.2: Check that the data is correct
            val readReposResult = ctx.selectFrom(GITHUB_REPOSITORY).fetchOne()
            expectThat(readReposResult).withNotNull {
                get { this.fullName }.isEqualTo("some-coder/my-weather-app")
                get { this.url }.isEqualTo("https://github.com/some-coder/my-weather-app")
            }

            // Step 2: Check workspace was created and linked
            // 2.1: Check there is one workspace
            expectThat(ctx.fetchCount(WORKSPACE)).isEqualTo(1)
            // 2.2: Check the workspace's name and description matches the provided structurizr
            val readWorkspacesResult = ctx.selectFrom(WORKSPACE).fetchOne()
            expectThat(readWorkspacesResult).withNotNull {
                get { this.name }.isEqualTo("My-weather-app")
                get { this.description }
                    .isEqualTo(
                        "A simple yet powerful weather app with a database and an HTTP controller that returns cool JSON data.",
                    )
            }
            // 2.3 Check that the workspace was linked correctly (inspect the repo's foreign key)
            expectThat(readReposResult).withNotNull {
                get { this.workspaceId }.isNotNull().isEqualTo(readWorkspacesResult?.id)
            }

            // Checking the actual parsing is done elsewhere
        }

        @Test
        fun `throws for bad structurizr`() {
            val rawStructurizr =
                """
                I AM SOME BAD STRUCTURIZR CODE!!! {{{{
                """.trimIndent()
            val repoName = "some-coder/my-weather-app"
            val repoUrl = "https://github.com/some-coder/my-weather-app"
            expectThrows<StructurizrDslParserException> {
                sut.initializeWorkspace(repoName, repoUrl, rawStructurizr)
            }.and {
                get { this.localizedMessage }
                    .isEqualTo(
                        "Unexpected tokens (expected: workspace) at line 1: I AM SOME BAD STRUCTURIZR CODE!!! {{{{",
                    )
            }

            // check that repo was not created

            expectThat(ctx.fetchCount(GITHUB_REPOSITORY)).isEqualTo(0)
        }

        @Test
        fun `handles repo already existing`() {
            val repoName = "some-coder/my-weather-app"
            val repoUrl = "https://github.com/some-coder/my-weather-app"
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
            gitRepo(createWorkspace = false) {
                this.url = repoUrl
                this.fullName = repoName
            }.create(ctx)

            sut.initializeWorkspace(repoName, repoUrl, rawStructurizr)

            // check there is only one repo still
            expectThat(ctx.fetchCount(GITHUB_REPOSITORY)).isEqualTo(1)
            // check that there is one workspace
            expectThat(ctx.fetchCount(WORKSPACE)).isEqualTo(1)
            val workspaceId = ctx.select(WORKSPACE.ID).from(WORKSPACE).fetchOne()
            val fetchRepoResult = ctx.selectFrom(GITHUB_REPOSITORY).fetchOne()

            expectThat(fetchRepoResult).withNotNull {
                get { this.workspaceId }.isEqualTo(workspaceId?.get(WORKSPACE.ID))
            }
        }

        @Test
        fun `handles repo AND workspace already existing`() {
            val workspaceId = UUID.randomUUID()
            val repoName = "my-fancy-repo"
            val repoUrl = "https://github.com/some-coder/my-weather-app"
            val parentId = UUID.randomUUID()
            val repoComponentId = UUID.randomUUID()
            val controllerComponentId = UUID.randomUUID()
            val loggerComponentId = UUID.randomUUID()
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
            // Create an existing repo AND a workspace with some members and relationships

            gitRepo {
                this.fullName = repoName
                this.url = repoUrl
                this.workspace { this.id = workspaceId }
            }.create(ctx)

            member {
                this.id = parentId
                this.workspace { this.id = workspaceId }
                this.name = "my-software-system"
            }.create(ctx)

            relationship {
                this.workspace { this.id = workspaceId }
                this.startMember {
                    this.name = "my-OLD-controller"
                    this.id = controllerComponentId
                    this.workspace { this.id = workspaceId }
                    this.parentId = parentId
                    this.description = "handles HTTP requests"
                }
                this.endMember {
                    this.name = "my-OLD-repository"
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

            // act
            sut.initializeWorkspace(repoName, repoUrl, rawStructurizr)

            // Step 1: check that none of the old components remain
            val componentIds =
                ctx
                    .select(MEMBER.ID)
                    .from(MEMBER)
                    .fetch()
                    .map { it.get(MEMBER.ID) }
            val componentNames =
                ctx
                    .select(MEMBER.NAME)
                    .from(MEMBER)
                    .fetch()
                    .map { it.get(MEMBER.NAME) }
            expect {
                that(componentIds).doesNotContain(controllerComponentId, repoComponentId, loggerComponentId)
                that(componentNames).none { contains("OLD") }
            }

            // Step 2: Check workspace was created and linked
            // 2.1: Check there is one workspace
            expectThat(ctx.fetchCount(WORKSPACE)).isEqualTo(1)
            // 2.2: Check the workspace's name and description matches the provided structurizr
            val readWorkspacesResult = ctx.selectFrom(WORKSPACE).fetchOne()
            expectThat(readWorkspacesResult).withNotNull {
                get { this.name }.isEqualTo("My-weather-app")
                get { this.description }
                    .isEqualTo(
                        "A simple yet powerful weather app with a database and an HTTP controller that returns cool JSON data.",
                    )
            }
            // 2.3 Check that the workspace was linked correctly (inspect the repo's foreign key)
            val readReposResult = ctx.selectFrom(GITHUB_REPOSITORY).fetchOne()
            expectThat(readReposResult).withNotNull {
                get { this.fullName }.isEqualTo(repoName)
                get { this.url }.isEqualTo("https://github.com/some-coder/my-weather-app")
            }
            expectThat(readReposResult).withNotNull {
                get { this.workspaceId }.isNotNull().isEqualTo(readWorkspacesResult?.id)
            }
        }
    }
}

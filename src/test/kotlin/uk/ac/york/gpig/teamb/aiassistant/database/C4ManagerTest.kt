package uk.ac.york.gpig.teamb.aiassistant.database

import org.jooq.DSLContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.york.gpig.teamb.aiassistant.testutils.AiAssistantTest
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.GitRepoBuilder.Companion.gitRepo
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.MemberBuilder.Companion.member
import uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders.RelationshipBuilder.Companion.relationship
import java.io.File
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

            println(sut.gitRepoToStructurizrDsl(repoName))
        }
    }

    @Nested
    @DisplayName("Tests for consuming a C4 model and storing it in the database")
    inner class ConsumeC4ModelTest {
        @Test
        fun `smoke test`() {
            val rawStructurizr = File("src/test/resources/structurizr/weather-app.dsl").readText()
            sut.consumeStructurizrWorkspace(
                "my-owner/my-fancy-repo",
                rawStructurizr,
            )
        }
    }
}

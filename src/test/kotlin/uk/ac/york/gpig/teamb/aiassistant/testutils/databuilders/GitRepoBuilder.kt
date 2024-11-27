package uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders

import org.jooq.DSLContext
import uk.ac.york.gpig.teamb.aiassistant.tables.references.GITHUB_REPOSITORY
import java.util.UUID

@TestDSL
class GitRepoBuilder(val createWorkspace: Boolean) : TestDataWithIdBuilder<GitRepoBuilder, UUID?>() {
    override var id: UUID = UUID.randomUUID()
    var url: String = "https://github.com/some-coder/my-fancy-repo"
    var fullName: String = "some-coder/my-fancy-repo"

    companion object {
        @TestDSL
        fun gitRepo(
            createWorkspace: Boolean = true,
            setup: GitRepoBuilder.() -> Unit,
        ): GitRepoBuilder = GitRepoBuilder(createWorkspace).apply(setup)
    }

    private val workspaceId = UUID.randomUUID()

    var workspaceBuilder: WorkspaceBuilder.() -> Unit = {
        this.id = this@GitRepoBuilder.workspaceId
    }

    fun workspace(setup: WorkspaceBuilder.() -> Unit): GitRepoBuilder {
        workspaceBuilder = setup
        return this
    }

    override fun create(ctx: DSLContext): GitRepoBuilder =
        this.create(
            ctx,
            GITHUB_REPOSITORY,
            GITHUB_REPOSITORY.ID,
        ) {
            val workspace = if (createWorkspace) WorkspaceBuilder.workspace(workspaceBuilder).create(ctx) else null
            ctx.insertInto(GITHUB_REPOSITORY)
                .columns(
                    GITHUB_REPOSITORY.ID,
                    GITHUB_REPOSITORY.FULL_NAME,
                    GITHUB_REPOSITORY.URL,
                    GITHUB_REPOSITORY.WORKSPACE_ID,
                )
                .values(
                    id,
                    fullName,
                    url,
                    workspace?.id,
                )
                .execute()
        }
}

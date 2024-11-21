package uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders

import org.jooq.DSLContext
import uk.ac.york.gpig.teamb.aiassistant.tables.references.WORKSPACE
import java.util.UUID

class WorkspaceBuilder : TestDataWithIdBuilder<WorkspaceBuilder, UUID?>() {
    override var id: UUID? = UUID.randomUUID()
    var name: String = "My fancy workspace"
    var description: String? = "My fancy description. Very cool!"

    companion object {
        @TestDSL
        fun workspace(setup: WorkspaceBuilder.() -> Unit): WorkspaceBuilder = WorkspaceBuilder().apply(setup)
    }

    override fun create(ctx: DSLContext): WorkspaceBuilder =
        this.create(
            ctx,
            WORKSPACE,
            WORKSPACE.ID,
        ) {
            ctx.insertInto(WORKSPACE)
                .columns(
                    WORKSPACE.ID,
                    WORKSPACE.NAME,
                    WORKSPACE.DESCRIPTION,
                )
                .values(
                    id,
                    name,
                    description,
                )
                .execute()
        }
}

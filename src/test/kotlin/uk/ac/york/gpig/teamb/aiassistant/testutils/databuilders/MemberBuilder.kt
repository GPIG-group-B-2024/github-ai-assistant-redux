package uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders

import org.jooq.DSLContext
import uk.ac.york.gpig.teamb.aiassistant.enums.MemberType
import uk.ac.york.gpig.teamb.aiassistant.tables.references.MEMBER
import java.util.UUID

@TestDSL
class MemberBuilder : TestDataWithIdBuilder<MemberBuilder, UUID?>() {
    override var id: UUID? = UUID.randomUUID()
    var type: MemberType = MemberType.COMPONENT
    var name: String = "my-controller"
    var description: String? = "handles incoming HTTP requests"
    private val workspaceId = UUID.randomUUID()

    var workspaceBuilder: WorkspaceBuilder.() -> Unit = { this.id = this@MemberBuilder.workspaceId }

    fun workspace(setup: WorkspaceBuilder.() -> Unit): MemberBuilder {
        workspaceBuilder = setup
        return this
    }

    var parentId: UUID? = null // TODO: see if adding a parent builder is at all useful

    companion object {
        @TestDSL
        fun member(setup: MemberBuilder.() -> Unit): MemberBuilder = MemberBuilder().apply(setup)
    }

    override fun create(ctx: DSLContext): MemberBuilder =
        this.create(ctx, MEMBER, MEMBER.ID) {
            val workspace = WorkspaceBuilder.workspace(workspaceBuilder).create(ctx)
            ctx
                .insertInto(MEMBER)
                .columns(
                    MEMBER.ID,
                    MEMBER.NAME,
                    MEMBER.TYPE,
                    MEMBER.DESCRIPTION,
                    MEMBER.PARENT,
                    MEMBER.WORKSPACE_ID,
                ).values(id, name, type, description, parentId, workspace.id)
                .execute()
        }
}

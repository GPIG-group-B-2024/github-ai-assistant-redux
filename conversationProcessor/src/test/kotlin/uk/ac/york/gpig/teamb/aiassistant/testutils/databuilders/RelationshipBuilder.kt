package uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders

import org.jooq.DSLContext
import uk.ac.york.gpig.teamb.aiassistant.tables.references.RELATIONSHIP
import java.util.UUID

@TestDSL
class RelationshipBuilder : TestDataBuilder<RelationshipBuilder>() {
    companion object {
        @TestDSL
        fun relationship(setup: RelationshipBuilder.() -> Unit): RelationshipBuilder = RelationshipBuilder().apply(setup)
    }

    private val startMemberId = UUID.randomUUID()
    private val endMemberId = UUID.randomUUID()
    var description: String? = "uses"

    var startMemberBuilder: MemberBuilder.() -> Unit = {
        this.id = this@RelationshipBuilder.startMemberId
    }
    var endMemberBuilder: MemberBuilder.() -> Unit = {
        this.id = this@RelationshipBuilder.endMemberId
    }

    fun startMember(setup: MemberBuilder.() -> Unit): RelationshipBuilder {
        startMemberBuilder = setup
        return this
    }

    fun endMember(setup: MemberBuilder.() -> Unit): RelationshipBuilder {
        endMemberBuilder = setup
        return this
    }

    private val workspaceId = UUID.randomUUID()

    var workspaceBuilder: WorkspaceBuilder.() -> Unit = {
        this.id = this@RelationshipBuilder.workspaceId
    }

    fun workspace(setup: WorkspaceBuilder.() -> Unit): RelationshipBuilder {
        workspaceBuilder = setup
        return this
    }

    fun getStartMemberId() = MemberBuilder.member(startMemberBuilder).id

    fun getEndMemberId() = MemberBuilder.member(endMemberBuilder).id

    override fun create(ctx: DSLContext): RelationshipBuilder =
        this.create(
            ctx,
            RELATIONSHIP,
            { RELATIONSHIP.START_MEMBER.eq(startMemberId).and(RELATIONSHIP.END_MEMBER.eq(endMemberId)) },
        ) {
            val startMember = MemberBuilder.member(startMemberBuilder).create(ctx)
            val endMember = MemberBuilder.member(endMemberBuilder).create(ctx)
            val workspace = WorkspaceBuilder.workspace(workspaceBuilder).create(ctx)
            ctx
                .insertInto(RELATIONSHIP)
                .columns(
                    RELATIONSHIP.START_MEMBER,
                    RELATIONSHIP.END_MEMBER,
                    RELATIONSHIP.WORKSPACE_ID,
                    RELATIONSHIP.DESCRIPTION,
                ).values(startMember.id, endMember.id, workspace.id, description)
                .execute()
        }
}

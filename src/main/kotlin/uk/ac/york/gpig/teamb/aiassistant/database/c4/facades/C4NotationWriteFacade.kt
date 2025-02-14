package uk.ac.york.gpig.teamb.aiassistant.database.c4.facades

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4ElementEntity
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4RelationshipEntity
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4WorkspaceEntity
import uk.ac.york.gpig.teamb.aiassistant.tables.references.GITHUB_REPOSITORY
import uk.ac.york.gpig.teamb.aiassistant.tables.references.MEMBER
import uk.ac.york.gpig.teamb.aiassistant.tables.references.RELATIONSHIP
import uk.ac.york.gpig.teamb.aiassistant.tables.references.WORKSPACE
import java.util.UUID

/**
 * Handles writing to the database storing the C4 representations of repositories.
 * */
@Repository
class C4NotationWriteFacade(
    val ctx: DSLContext,
) {
    fun writeMemberList(entities: List<C4ElementEntity>) {
        val success =
            ctx.insertInto(MEMBER)
                .columns(MEMBER.ID, MEMBER.NAME, MEMBER.DESCRIPTION, MEMBER.PARENT, MEMBER.TYPE, MEMBER.WORKSPACE_ID)
                .valuesOfRows(
                    entities.map { DSL.row(it.id, it.name, it.description, it.parentId, it.type, it.workspaceId) },
                )
                .execute() == entities.size
        if (!success) throw Exception("Failed to write entity records")
    }

    fun writeRelationshipsList(relationships: List<C4RelationshipEntity>) {
        val success =
            ctx.insertInto(RELATIONSHIP)
                .columns(RELATIONSHIP.START_MEMBER, RELATIONSHIP.END_MEMBER, RELATIONSHIP.WORKSPACE_ID, RELATIONSHIP.DESCRIPTION)
                .valuesOfRows(
                    relationships.map {
                        DSL.row(
                            it.from,
                            it.to,
                            it.workspaceId,
                            it.description,
                        )
                    },
                )
                .execute() == relationships.size
        if (!success) throw Exception("Failed to write relationships list")
    }

    fun writeWorkspace(workspace: C4WorkspaceEntity) {
        val success =
            ctx.insertInto(WORKSPACE)
                .columns(WORKSPACE.ID, WORKSPACE.NAME, WORKSPACE.DESCRIPTION)
                .values(workspace.id, workspace.name, workspace.description)
                .execute() == 1
        if (!success) throw Exception("Failed to write workspace record with id ${workspace.id} (${workspace.name})")
    }

    fun linkRepoToWorkspace(
        repoName: String,
        workspaceId: UUID,
    ) {
        val success =
            ctx.update(GITHUB_REPOSITORY)
                .set(GITHUB_REPOSITORY.WORKSPACE_ID, workspaceId)
                .where(GITHUB_REPOSITORY.FULL_NAME.eq(repoName))
                .execute() == 1
        if (!success) throw Exception("Failed to link repository $repoName with workspace $workspaceId")
    }
}

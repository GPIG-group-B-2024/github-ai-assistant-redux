package uk.ac.york.gpig.teamb.aiassistant.database.facades

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import uk.ac.york.gpig.teamb.aiassistant.database.entities.C4ElementEntity
import uk.ac.york.gpig.teamb.aiassistant.tables.references.MEMBER

/**
 * Handles writing to the database storing the C4 representations of repositories.
 * */
@Repository
class C4NotationWriteFacade(
    val ctx: DSLContext,
) {
    fun writeMember(member: C4ElementEntity) {
        val success =
            ctx.insertInto(MEMBER)
                .columns(
                    MEMBER.ID,
                    MEMBER.WORKSPACE_ID,
                    MEMBER.NAME,
                    MEMBER.DESCRIPTION,
                    MEMBER.PARENT,
                    MEMBER.TYPE,
                )
                .values(
                    member.id,
                    member.workspaceId,
                    member.name,
                    member.description,
                    member.parentId,
                    member.type,
                ).execute() == 1
        if (!success) throw Exception("Failed to write member ${member.id}")
    }
}

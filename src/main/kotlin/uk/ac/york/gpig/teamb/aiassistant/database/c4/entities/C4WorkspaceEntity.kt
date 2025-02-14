package uk.ac.york.gpig.teamb.aiassistant.database.c4.entities

import org.jooq.Record
import uk.ac.york.gpig.teamb.aiassistant.tables.references.WORKSPACE
import java.util.UUID

data class C4WorkspaceEntity(
    val id: UUID,
    val name: String,
    val description: String?,
) {
    companion object {
        fun fromJooq(record: Record): C4WorkspaceEntity =
            C4WorkspaceEntity(
                id = record.get(WORKSPACE.ID)!!,
                name = record.get(WORKSPACE.NAME)!!,
                description = record.get(WORKSPACE.DESCRIPTION),
            )
    }

    fun toStructurizrString(): String =
        when (this.description) {
            null -> "workspace \"$name\""
            else -> "workspace \"$name\" \"$description\""
        }
}

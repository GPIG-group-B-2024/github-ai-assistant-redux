package uk.ac.york.gpig.teamb.aiassistant.database.entities

import org.jooq.Record
import uk.ac.york.gpig.teamb.aiassistant.tables.references.MEMBER
import uk.ac.york.gpig.teamb.aiassistant.tables.references.RELATIONSHIP
import java.util.UUID

data class C4RelationshipEntity(
    val from: UUID,
    val to: UUID,
    val fromName: String,
    val toName: String,
    val description: String?,
    val workspaceId: UUID,
) {
    companion object {
        /**
         * Database alias for joining with the MEMBER table to find the start member name
         * */
        val fromMember = MEMBER.`as`("m1")

        /**
         * Database alias for joining with the MEMBER table to find the end member name
         * */
        val toMember = MEMBER.`as`("m2")

        fun fromJooq(record: Record): C4RelationshipEntity =
            C4RelationshipEntity(
                from = record.get(RELATIONSHIP.START_MEMBER)!!,
                to = record.get(RELATIONSHIP.END_MEMBER)!!,
                description = record.get(RELATIONSHIP.DESCRIPTION),
                fromName = record.get(fromMember.NAME)!!,
                toName = record.get(toMember.NAME)!!,
                workspaceId = record.get(RELATIONSHIP.WORKSPACE_ID)!!,
            )
    }

    /**
     * Convert relationship entity to structurizr notation
     * */
    fun toStructurizrString(): String =
        when (this.description) {
            null -> "$fromName -> $toName"
            else -> "$fromName -> $toName \"$description\""
        }
}

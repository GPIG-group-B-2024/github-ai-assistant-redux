package uk.ac.york.gpig.teamb.aiassistant.database.entities

import org.jooq.Record
import uk.ac.york.gpig.teamb.aiassistant.tables.references.RELATIONSHIP
import java.util.UUID

data class C4RelationshipEntity(
    val from: UUID,
    val to: UUID,
    val description: String?,
) {
    companion object {
        fun fromJooq(record: Record): C4RelationshipEntity =
            C4RelationshipEntity(
                from = record.get(RELATIONSHIP.START_MEMBER)!!,
                to = record.get(RELATIONSHIP.END_MEMBER)!!,
                description = record.get(RELATIONSHIP.DESCRIPTION),
            )
    }

    /**
     * Convert relationship entity to structurizr notation
     * */
    fun toStructurizrString(): String =
        when (this.description) {
            null -> "$from -> $to"
            else -> "$from -> $to \"$description\""
        }
}

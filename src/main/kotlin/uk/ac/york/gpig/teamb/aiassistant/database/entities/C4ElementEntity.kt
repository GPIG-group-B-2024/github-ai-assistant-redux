package uk.ac.york.gpig.teamb.aiassistant.database.entities

import org.jooq.Record
import uk.ac.york.gpig.teamb.aiassistant.enums.MemberType
import uk.ac.york.gpig.teamb.aiassistant.tables.references.MEMBER
import java.util.UUID

data class C4ElementEntity(
    val id: UUID,
    val parentId: UUID?,
    val type: MemberType,
    val name: String,
    val description: String?,
    val workspaceId: UUID,
) {
    /**
     * The variable associated with this element in structurizr DSL
     * */
    val variableName = this.name.replace("[\\s.]]".toRegex(), "-") // replace dots and spaces with dashes

    companion object {
        fun fromJooq(record: Record): C4ElementEntity =
            C4ElementEntity(
                id = record.get(MEMBER.ID)!!,
                parentId = record.get(MEMBER.PARENT),
                type = record.get(MEMBER.TYPE)!!,
                name = record.get(MEMBER.NAME)!!,
                description = record.get(MEMBER.DESCRIPTION),
                workspaceId = record.get(MEMBER.WORKSPACE_ID)!!,
            )
    }
}

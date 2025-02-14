package uk.ac.york.gpig.teamb.aiassistant.database.c4.entities

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.UUID

class C4RelationshipEntityTest {
    @Test
    fun `handles non-null relationship description`() {
        val fromId = UUID.randomUUID()
        val toId = UUID.randomUUID()

        val relationship =
            C4RelationshipEntity(
                from = fromId,
                to = toId,
                description = "my fancy description",
                fromName = "component-1",
                toName = "component-2",
                workspaceId = UUID.randomUUID(),
            )

        expectThat(relationship.toStructurizrString()).isEqualTo("component-1 -> component-2 \"my fancy description\"")
    }

    @Test
    fun `handles lack of description`() {
        val fromId = UUID.randomUUID()
        val toId = UUID.randomUUID()

        val relationship =
            C4RelationshipEntity(
                from = fromId,
                to = toId,
                description = null,
                fromName = "component-1",
                toName = "component-2",
                workspaceId = UUID.randomUUID(),
            )
        expectThat(relationship.toStructurizrString()).isEqualTo("component-1 -> component-2")
    }
}

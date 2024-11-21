package uk.ac.york.gpig.teamb.aiassistant.database.entities

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
            )

        expectThat(relationship.toStructurizrString()).isEqualTo("$fromId -> $toId \"my fancy description\"")
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
            )
        expectThat(relationship.toStructurizrString()).isEqualTo("$fromId -> $toId")
    }
}

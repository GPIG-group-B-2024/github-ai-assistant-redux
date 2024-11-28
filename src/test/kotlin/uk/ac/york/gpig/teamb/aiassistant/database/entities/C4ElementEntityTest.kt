package uk.ac.york.gpig.teamb.aiassistant.database.entities

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.ac.york.gpig.teamb.aiassistant.enums.MemberType
import java.util.UUID

class C4ElementEntityTest {
    @Test
    fun `formats variable name to exclude periods and spaces`()  {
        val entity =
            C4ElementEntity(
                id = UUID.randomUUID(),
                parentId = UUID.randomUUID(),
                type = MemberType.COMPONENT,
                name = "com.test.my component",
                description = "My description",
                workspaceId = UUID.randomUUID(),
            )

        expectThat(entity.variableName).isEqualTo("com-test-my-component")
    }
}

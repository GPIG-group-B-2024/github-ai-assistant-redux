package uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders

import org.jooq.DSLContext
import uk.ac.york.gpig.teamb.aiassistant.enums.LlmMessageRole
import uk.ac.york.gpig.teamb.aiassistant.tables.references.LLM_MESSAGE
import java.time.OffsetDateTime
import java.util.UUID

class LLMMessageBuilder : TestDataWithIdBuilder<LLMMessageBuilder, UUID?>() {
    override var id: UUID? = UUID.randomUUID()
    var role: LlmMessageRole = LlmMessageRole.USER
    var content: String = "Some cool message"
    var createdAt: OffsetDateTime = OffsetDateTime.now()

    companion object {
        @TestDSL
        fun message(setup: LLMMessageBuilder.() -> Unit) = LLMMessageBuilder().apply(setup)
    }

    override fun create(ctx: DSLContext): LLMMessageBuilder =
        this.create(ctx, LLM_MESSAGE, LLM_MESSAGE.ID) {
            ctx.insertInto(LLM_MESSAGE)
                .columns(
                    LLM_MESSAGE.ID,
                    LLM_MESSAGE.ROLE,
                    LLM_MESSAGE.CONTENT,
                    LLM_MESSAGE.CREATED_AT,
                )
                .values(id, role, content, createdAt)
                .execute()
        }
}

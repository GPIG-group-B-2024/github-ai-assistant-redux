package uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders

import org.jooq.DSLContext
import org.jooq.impl.DSL
import uk.ac.york.gpig.teamb.aiassistant.tables.references.CONVERSATION_MESSAGE
import java.util.UUID

@TestDSL
class LLMConversationMessageBuilder : TestDataBuilder<LLMConversationMessageBuilder>() {
    private val conversationId = UUID.randomUUID()
    private val messageId = UUID.randomUUID()

    var conversationBuilder: LLMConversationBuilder.() -> Unit = {
        this.id = this@LLMConversationMessageBuilder.conversationId
    }
    private var hasMultipleMessages = false

    var firstMessageBuilder: LLMMessageBuilder.() -> Unit = {
        this.id = this@LLMConversationMessageBuilder.messageId
    }

    val messageBuilders = mutableListOf(firstMessageBuilder)

    companion object {
        @TestDSL
        fun conversationWithMessages(setup: LLMConversationMessageBuilder.() -> Unit) = LLMConversationMessageBuilder().apply(setup)
    }

    fun message(setup: LLMMessageBuilder.() -> Unit): LLMConversationMessageBuilder {
        if (!hasMultipleMessages) { // the user has only specified 1 message builder, modify the default
            // one
            messageBuilders[0] = setup
            hasMultipleMessages = true
            // any additional message builders will be added to the conversation instead of modifying the
            // first message
        } else {
            messageBuilders.add(setup)
        }
        return this
    }

    fun conversation(setup: LLMConversationBuilder.() -> Unit): LLMConversationMessageBuilder {
        conversationBuilder = setup
        return this
    }

    override fun create(ctx: DSLContext): LLMConversationMessageBuilder =
        this.create(
            ctx = ctx,
            table = CONVERSATION_MESSAGE,
            existenceCheck = { CONVERSATION_MESSAGE.CONVERSATION_ID.eq(conversationId) },
            expectedInsertCount = messageBuilders.size,
        ) {
            // create conversation
            val conversation = LLMConversationBuilder.conversation(conversationBuilder).create(ctx)
            // create a message from each of the builders and prepare a SQL row linking each one to the
            // conversation
            val messageLinkRows =
                messageBuilders.map {
                    DSL.row(conversation.id, LLMMessageBuilder.message(it).create(ctx).id)
                }
            // write links to db
            ctx
                .insertInto(CONVERSATION_MESSAGE)
                .columns(CONVERSATION_MESSAGE.CONVERSATION_ID, CONVERSATION_MESSAGE.MESSAGE_ID)
                .valuesOfRows(messageLinkRows)
                .execute()
        }
}

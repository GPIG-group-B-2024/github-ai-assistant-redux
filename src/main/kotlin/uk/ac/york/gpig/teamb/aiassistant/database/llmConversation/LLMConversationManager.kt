package uk.ac.york.gpig.teamb.aiassistant.database.llmConversation

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.ac.york.gpig.teamb.aiassistant.database.c4.facades.C4NotationReadFacade
import uk.ac.york.gpig.teamb.aiassistant.database.exceptions.NotFoundException.NotFoundByIdException
import uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.conversions.toJooqMessageRole
import uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.facades.LLMConversationReadFacade
import uk.ac.york.gpig.teamb.aiassistant.database.llmConversation.facades.LLMConversationWriteFacade
import uk.ac.york.gpig.teamb.aiassistant.llm.client.OpenAIMessage
import java.util.UUID

@Service
class LLMConversationManager(
    @Autowired
    private val llmConversationWriteFacade: LLMConversationWriteFacade,
    @Autowired
    private val llmConversationReadFacade: LLMConversationReadFacade,
    @Autowired
    private val c4NotationReadFacade: C4NotationReadFacade,
    @Autowired
    private val transactionTemplate: TransactionTemplate,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Start a new conversation and add the first message to it.
     * */
    fun initConversationWithFirstMessage(
        repoId: UUID,
        issueId: Int,
        role: OpenAIMessage.Role,
        content: String,
    ) {
        when {
            !c4NotationReadFacade.checkRepositoryExists(repoId) -> throw NotFoundByIdException(repoId, "Git repo")
            llmConversationReadFacade.checkConversationExists(
                repoId,
                issueId,
            ) -> throw IllegalStateException("Conversation about issue $issueId in repo $repoId already exists")
        }
        val conversationId = UUID.randomUUID()
        val messageId = UUID.randomUUID()

        transactionTemplate.execute {
            llmConversationWriteFacade.initConversation(conversationId, repoId, issueId)
            llmConversationWriteFacade.storeMessage(messageId, role.toJooqMessageRole(), content)
            llmConversationWriteFacade.linkMessageToConversation(conversationId, messageId)
        }
        logger.info("Stored new conversation with id $conversationId and first message with id $messageId")
    }

    fun fetchConversations() = llmConversationReadFacade.fetchConversations().also { logger.info("Found ${it.size} conversations") }

    fun fetchConversationMessages(conversationId: UUID) =
        llmConversationReadFacade.listConversationMessages(conversationId).also {
            logger.info("Found ${it.size} messages in conversation $conversationId")
        }
}

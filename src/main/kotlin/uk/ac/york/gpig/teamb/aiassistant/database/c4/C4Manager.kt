package uk.ac.york.gpig.teamb.aiassistant.database.c4

import com.structurizr.dsl.StructurizrDslParser
import com.structurizr.dsl.StructurizrDslParserException
import com.structurizr.model.Component
import com.structurizr.model.Container
import com.structurizr.model.Element
import com.structurizr.model.Person
import com.structurizr.model.Relationship
import com.structurizr.model.SoftwareSystem
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import uk.ac.york.gpig.teamb.aiassistant.database.c4.conversions.toStructurizrString
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4ElementEntity
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4RelationshipEntity
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4WorkspaceEntity
import uk.ac.york.gpig.teamb.aiassistant.database.c4.facades.C4NotationReadFacade
import uk.ac.york.gpig.teamb.aiassistant.database.c4.facades.C4NotationWriteFacade
import uk.ac.york.gpig.teamb.aiassistant.database.exceptions.NotFoundException
import uk.ac.york.gpig.teamb.aiassistant.enums.MemberType
import java.util.UUID

@Service
class C4Manager(
    private val c4NotationReadFacade: C4NotationReadFacade,
    private val c4NotationWriteFacade: C4NotationWriteFacade,
    private val transactionTemplate: TransactionTemplate,
) {
    private val logger = LoggerFactory.getLogger(C4Manager::class.java)

    /**
     * Convert a structurizr workspace entity to a valid structurizr-compliant string for passing
     * forward to an LLM.
     *
     * Note: doesn't include a `views` or `styles` block (doesn't prevent it from being compiled)
     */
    fun gitRepoToStructurizrDsl(repoName: String): String {
        val workspace =
            c4NotationReadFacade.getRepositoryWorkspace(
                repoName,
            ) ?: throw NotFoundException.NotFoundByNameException(repoName, "github repository")
        logger.info("Found repository with name $repoName")
        val members = c4NotationReadFacade.getMembers(workspace.id)
        logger.info("Found ${members.size} members in repo $repoName")
        val relationships = c4NotationReadFacade.getRelationships(workspace.id)
        logger.info("Found ${relationships.size} relationships in repo $repoName")
        val memberToChildren = members.groupBy { it.parentId }
        val componentsBlock =
            memberToChildren[null]!!.joinToString("\n|\t\t") {
                it.printWithChildren(memberToChildren, 2)
            }
        val relationshipsBlock = relationships.joinToString("\n|\t\t") { it.toStructurizrString() }
        return """
            |${workspace.toStructurizrString()}{
            |   model {
            |       $componentsBlock
            |       $relationshipsBlock
            |   }
            |}
            """.trimMargin()
    }

    /**
     * Convert a `Parent id -> list of children` map to a list of c4 entities, preserving parent-child
     * relationships.
     */
    private fun Map<String?, List<Element>>.toEntityList(
        workspaceId: UUID,
        idMappings: Map<String, UUID>,
    ) = this.flatMap { (parentId, children) ->
        val parentUUID = if (parentId != null) idMappings[parentId]!! else null
        children.map {
            C4ElementEntity(
                id = idMappings[it.id]!!,
                parentId = parentUUID,
                type =
                    when (it) {
                        // structurizr parser doesn't store the type of elements
                        // and instead casts them into different classes
                        is Person -> MemberType.PERSON
                        is SoftwareSystem -> MemberType.SOFTWARE_SYSTEM
                        is Container -> MemberType.CONTAINER
                        is Component -> MemberType.COMPONENT
                        else ->
                            throw IllegalArgumentException(
                                "Unknown member type: ${it::class.qualifiedName}",
                            )
                    },
                name = it.name,
                description = it.description,
                workspaceId = workspaceId,
            )
        }
    }

    private fun Set<Relationship>.toRelationshipEntityList(
        workspaceId: UUID,
        idMappings: Map<String, UUID>,
    ) = this.map {
        C4RelationshipEntity(
            from = idMappings[it.sourceId]!!,
            to = idMappings[it.destinationId]!!,
            fromName = it.source.name,
            toName = it.destination.name,
            description = it.description,
            workspaceId = workspaceId,
        )
    }

    /**
     * Consumes a structurizr file representing a single workspace and stores result to database. If
     * the file is correct, the following entities will be created:
     * * A `Member` for each C4 component
     * * A `Relationship` for each relationship
     * * A `Workspace` entity linked to the provided github repo name
     */
    internal fun consumeStructurizrWorkspace(
        repoName: String,
        rawInput: String,
    ) {
        if (!c4NotationReadFacade.checkRepositoryExists(repoName)) {
            // we don't know of this repository - throw exception and abort early
            throw NotFoundException.NotFoundByNameException(repoName, "github repository")
        }
        logger.info("Found repository with name $repoName")

        val parser = StructurizrDslParser()
        try {
            parser.parse(rawInput)
        } catch (e: StructurizrDslParserException) {
            logger.error("Error processing structurizr payload: ${e.localizedMessage}")
            throw e
        }
        logger.info("Successfully parsed structurizr input")
        val workspace = parser.workspace
        val workspaceId = UUID.randomUUID()
        val workspaceEntity =
            C4WorkspaceEntity(
                id = workspaceId,
                name = workspace.name,
                description = workspace.description,
            )

        val rawComponents = workspace.model.elements
        logger.info("Found ${rawComponents.size} components")
        val rawComponentHierarchy =
            rawComponents.groupBy {
                it.parent?.id
            } // group parsed components by their parent ID (null for top-level components)
        val stringIdToDbUUID =
            rawComponents
                .map { it.id }
                .associateWith {
                    UUID.randomUUID()
                } // assign a unique UUID (to be stored in the database) to each discovered component
        // for each parentId (including null), create entities representing its children and associate
        // them with the parent ID
        val entities = rawComponentHierarchy.toEntityList(workspaceId, stringIdToDbUUID)

        val relationships =
            workspace.model.relationships.toRelationshipEntityList(workspaceId, stringIdToDbUUID)
        logger.info("Found ${relationships.size} relationships")

        // write new entities to database and link the new workspace to github repo
        // TODO: discuss how to handle several c4 workspace creations
        //  - do we delete the existing workspace? do we keep a list of workspaces (and creation date)
        // for each repo?
        transactionTemplate.execute {
            c4NotationWriteFacade.writeWorkspace(workspaceEntity)
            c4NotationWriteFacade.linkRepoToWorkspace(repoName, workspaceId)
            c4NotationWriteFacade.writeMemberList(entities)
            c4NotationWriteFacade.writeRelationshipsList(relationships)
        }
        logger.info("Database write successful")
    }

    fun getRepoId(repoName: String): UUID =
        c4NotationReadFacade.getRepoId(
            repoName,
        ) ?: throw NotFoundException.NotFoundByNameException(repoName, "github repo")

    /**
     * For manual app operation/testing: create record of a github repo and associate a structurizr
     * workspace with it.
     * - If the repo does not exist in the system, this method will create a new record.
     * - If both the repo and the workspace exist, the operation will __completely__ overwrite it
     *
     * @param rawStructurizr The structurizr code representing the repo. __Must__ be valid.
     */
    @Transactional(rollbackFor = [StructurizrDslParserException::class])
    fun initializeWorkspace(
        repoName: String,
        repoUrl: String,
        rawStructurizr: String,
    ) {
        // Step 1: check if repo exists and create a new one if not
        if (!c4NotationReadFacade.checkRepositoryExists(repoName)) {
            val repoId = UUID.randomUUID()
            logger.info(
                "Could not find repository with name $repoName, creating and assigning ID $repoId...",
            )
            c4NotationWriteFacade.writeRepository(repoId, repoName, repoUrl)
        } else {
            logger.info("Repository with name $repoName already exists, skipping creation step...")
        }
        // Step 2: check if there is a workspace associated with this repo
        val workspaceId = c4NotationReadFacade.getRepositoryWorkspace(repoName)?.id
        if (workspaceId != null) {
            logger.info("Found workspace associated with repo $repoName. Deleting...")
            c4NotationWriteFacade.deleteWorkspaceRelationships(workspaceId).let {
                logger.info("Deleted $it relationships from workspace $workspaceId")
            }
            c4NotationWriteFacade.deleteWorkspaceMembers(workspaceId).let {
                logger.info("Deleted $it members from workspace $workspaceId")
            }
            c4NotationWriteFacade.removeLinkToWorkspace(repoName, workspaceId)
            logger.info("Removed link between repo $repoName and workspace $workspaceId")
            c4NotationWriteFacade.deleteWorkspace(workspaceId)
            logger.info("Deleted workspace $workspaceId")
        }
        // Step 3: process the structurizr payload and link to the repo
        consumeStructurizrWorkspace(repoName, rawStructurizr)
    }

    /**
     * Print a C4 entity and its children, if any, in a nested block
     *
     * @param memberToChildren map of parent ID -> children
     * @param indent amount by which to offset component in the printed string (number of tabs)
     */
    internal fun C4ElementEntity.printWithChildren(
        memberToChildren: Map<UUID?, List<C4ElementEntity>>,
        indent: Int,
    ): String {
        val header =
            "${this.variableName} = ${this.type.toStructurizrString()} \"${this.name}\" \"${this.description}\""
        val children = memberToChildren[this.id]
        val indentString = "\t".repeat(indent)
        return header +
            when (children) {
                null -> ""
                else -> // spotless:off
                    """{
                    |$indentString${children.joinToString("\n|$indentString") { it.printWithChildren(memberToChildren, indent + 1) }}
                    |$indentString}
                    """.trimIndent()
                // spotless:on
            }
    }
}

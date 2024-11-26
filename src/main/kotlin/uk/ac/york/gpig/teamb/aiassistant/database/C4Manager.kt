package uk.ac.york.gpig.teamb.aiassistant.database

import com.structurizr.dsl.StructurizrDslParser
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.ac.york.gpig.teamb.aiassistant.database.entities.C4ElementEntity
import uk.ac.york.gpig.teamb.aiassistant.database.entities.C4RelationshipEntity
import uk.ac.york.gpig.teamb.aiassistant.database.entities.C4WorkspaceEntity
import uk.ac.york.gpig.teamb.aiassistant.database.exceptions.NotFoundException.NotFoundByNameException
import uk.ac.york.gpig.teamb.aiassistant.database.facades.C4NotationReadFacade
import uk.ac.york.gpig.teamb.aiassistant.database.facades.C4NotationWriteFacade
import uk.ac.york.gpig.teamb.aiassistant.enums.MemberType
import java.util.UUID

@Service
class C4Manager(
    private val c4NotationReadFacade: C4NotationReadFacade,
    private val c4NotationWriteFacade: C4NotationWriteFacade,
    private val transactionTemplate: TransactionTemplate,
) {
    fun gitRepoToStructurizrDsl(repoName: String): String {
        val workspace =
            c4NotationReadFacade.getRepositoryWorkspace(
                repoName,
            ) ?: throw NotFoundByNameException(repoName, "github repository")

        val members = c4NotationReadFacade.getMembers(workspace.id)
        val relationships = c4NotationReadFacade.getRelationships(workspace.id)

        val memberToChildren = members.groupBy { it.parentId }
        val componentsBlock = memberToChildren[null]!!.joinToString("\n|\t\t") { it.printWithChildren(memberToChildren, 1) }
        val relationshipsBlock = relationships.joinToString("\n|\t\t") { it.toStructurizrString() }
        return """
            |${workspace.toStructurizrString()}{
            |   $componentsBlock
            |   $relationshipsBlock
            |}
            """.trimMargin()
    }

    /**
     * Consumes a structurizr file representing a single workspace and stores result to database.
     * If the file is correct, the following entities will be created:
     *  * A `Member` for each C4 component
     *  * A `Relationship` for each relationship
     *  * A `Workspace` entity linked to the provided github repo name
     * */
    fun consumeStructurizrWorkspace(
        repoName: String,
        rawInput: String,
    ) {
        val parser = StructurizrDslParser()
        parser.parse(rawInput)
        val workspace = parser.workspace
        val workspaceId = UUID.randomUUID()

        val workspaceEntity =
            C4WorkspaceEntity(
                id = workspaceId,
                name = workspace.name,
                description = workspace.description,
            )

        val rawComponents = workspace.model.elements
        val rawComponentHierarchy =
            rawComponents.groupBy {
                it.parent?.id
            } // group parsed components by their parent ID (null for top-level components)
        val stringIdToDbUUID =
            rawComponents.map {
                it.id
            }.associateWith { UUID.randomUUID() } // assign a unique UUID (to be stored in the database) to each discovered component
        // for each parentId (including null), create entities representing its children and associate them with the parent ID
        val entities =
            rawComponentHierarchy.flatMap {
                    (parentId, children) ->
                val parentUUID = if (parentId != null) stringIdToDbUUID[parentId]!! else null
                children.map {
                    C4ElementEntity(
                        id = stringIdToDbUUID[it.id]!!,
                        parentId = parentUUID,
                        type = MemberType.COMPONENT,
                        name = it.name,
                        description = null,
                        workspaceId = workspaceId,
                    )
                }
            }

        val relationships =
            workspace.model.relationships.map {
                C4RelationshipEntity(
                    from = stringIdToDbUUID[it.sourceId]!!,
                    to = stringIdToDbUUID[it.destinationId]!!,
                    fromName = it.source.name,
                    toName = it.destination.name,
                    description = it.description,
                    workspaceId = workspaceId,
                )
            }

        transactionTemplate.execute {
            c4NotationWriteFacade.writeWorkspace(workspaceEntity)
            c4NotationWriteFacade.writeMemberList(entities)
            c4NotationWriteFacade.writeRelationshipsList(relationships)
        }
    }

    internal fun C4ElementEntity.printWithChildren(
        memberToChildren: Map<UUID?, List<C4ElementEntity>>,
        indent: Int,
    ): String {
        val header = "\"${this.name}\" \"${this.description}\""
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

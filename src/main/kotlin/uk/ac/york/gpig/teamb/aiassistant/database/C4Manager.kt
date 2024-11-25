package uk.ac.york.gpig.teamb.aiassistant.database

import org.springframework.stereotype.Service
import uk.ac.york.gpig.teamb.aiassistant.database.entities.C4ElementEntity
import uk.ac.york.gpig.teamb.aiassistant.database.exceptions.NotFoundException.NotFoundByNameException
import uk.ac.york.gpig.teamb.aiassistant.database.facades.C4NotationReadFacade
import java.util.UUID

@Service
class C4Manager(
    private val c4NotationReadFacade: C4NotationReadFacade,
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

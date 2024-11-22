package uk.ac.york.gpig.teamb.aiassistant.database

import org.springframework.stereotype.Service
import uk.ac.york.gpig.teamb.aiassistant.database.exceptions.NotFoundException.NotFoundByNameException
import uk.ac.york.gpig.teamb.aiassistant.database.facades.C4NotationReadFacade

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
        return """
            ${workspace.toStructurizrString()}{
            ${memberToChildren[null]!!.joinToString("\n") { "${it.name} {${memberToChildren[it.id]?.map{it.name} ?: ""}}" }}
            ${relationships.joinToString("\n") { it.toStructurizrString() }}
            }
            """.trimIndent()
    }
}

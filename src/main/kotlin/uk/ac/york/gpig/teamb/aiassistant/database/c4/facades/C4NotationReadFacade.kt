package uk.ac.york.gpig.teamb.aiassistant.database.c4.facades

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4ElementEntity
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4RelationshipEntity
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4RelationshipEntity.Companion.fromMember
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4RelationshipEntity.Companion.toMember
import uk.ac.york.gpig.teamb.aiassistant.database.c4.entities.C4WorkspaceEntity
import uk.ac.york.gpig.teamb.aiassistant.tables.references.GITHUB_REPOSITORY
import uk.ac.york.gpig.teamb.aiassistant.tables.references.MEMBER
import uk.ac.york.gpig.teamb.aiassistant.tables.references.RELATIONSHIP
import uk.ac.york.gpig.teamb.aiassistant.tables.references.WORKSPACE
import java.util.UUID

/**
 * Handles reading from the database storing the C4 representations of repositories.
 * */
@Repository
class C4NotationReadFacade(
    val ctx: DSLContext,
) {
    /**
     * Gets all known C4 components from a workspace (i.e. associated with a single git repository)
     *
     * Note: returns all components in a flat list, children and parents need to be re-organised in a later step
     * */
    fun getMembers(workspaceId: UUID): List<C4ElementEntity> =
        ctx.select()
            .from(MEMBER)
            .where(
                MEMBER.WORKSPACE_ID
                    .eq(workspaceId),
            ).fetch(C4ElementEntity.Companion::fromJooq)

    /**
     * Gets all known relationships from a workspace
     * */
    fun getRelationships(workspaceId: UUID): List<C4RelationshipEntity> =
        ctx.select()
            .from(RELATIONSHIP)
            .join(fromMember).on(RELATIONSHIP.START_MEMBER.eq(fromMember.ID))
            .join(toMember).on(RELATIONSHIP.END_MEMBER.eq(toMember.ID))
            .where(
                RELATIONSHIP.WORKSPACE_ID.eq(workspaceId),
            ).fetch(C4RelationshipEntity::fromJooq)

    /**
     * Get the structurizr workspace associated with the given github repository
     * @param repoName repository name in the `owner/repo` format
     * */
    fun getRepositoryWorkspace(repoName: String): C4WorkspaceEntity? =
        ctx.select()
            .from(WORKSPACE)
            .join(GITHUB_REPOSITORY)
            .on(GITHUB_REPOSITORY.WORKSPACE_ID.eq(WORKSPACE.ID))
            .where(GITHUB_REPOSITORY.FULL_NAME.eq(repoName))
            .fetchOne(C4WorkspaceEntity.Companion::fromJooq)

    /**
     * Check that a github repository with the given name exists in the database
     *
     * @param repoName repository name in the `owner/repo` format
     * */
    fun checkRepositoryExists(repoName: String): Boolean =
        ctx.fetchExists(GITHUB_REPOSITORY.where(GITHUB_REPOSITORY.FULL_NAME.eq(repoName)))

    /**
     * Check that a github repository with the given ID exists in the database
     * */
    fun checkRepositoryExists(repoId: UUID): Boolean = ctx.fetchExists(GITHUB_REPOSITORY.where(GITHUB_REPOSITORY.ID.eq(repoId)))

    fun getRepoId(repoName: String): UUID? =
        ctx.selectFrom(
            GITHUB_REPOSITORY,
        ).where(GITHUB_REPOSITORY.FULL_NAME.eq(repoName)).fetchOne(GITHUB_REPOSITORY.ID)
}

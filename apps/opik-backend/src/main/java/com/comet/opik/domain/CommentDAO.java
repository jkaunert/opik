package com.comet.opik.domain;

import com.comet.opik.api.Comment;
import com.comet.opik.infrastructure.db.TransactionTemplateAsync;
import com.google.common.base.Preconditions;
import com.google.inject.ImplementedBy;
import io.r2dbc.spi.Statement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.stringtemplate.v4.ST;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

import static com.comet.opik.domain.AsyncContextUtils.bindUserNameAndWorkspaceContext;
import static com.comet.opik.domain.AsyncContextUtils.bindWorkspaceIdToFlux;
import static com.comet.opik.domain.AsyncContextUtils.bindWorkspaceIdToMono;
import static com.comet.opik.utils.AsyncUtils.makeFluxContextAware;
import static com.comet.opik.utils.AsyncUtils.makeMonoContextAware;

@ImplementedBy(CommentDAOImpl.class)
public interface CommentDAO {

    @Getter
    @RequiredArgsConstructor
    enum EntityType {
        TRACE("trace", "traces");

        private final String type;
        private final String tableName;
    }

    Mono<Long> addComment(UUID commentId, UUID entityId, UUID projectId, Comment comment);

    Mono<Comment> findById(UUID traceId, UUID commentId);

    Mono<Void> updateComment(UUID commentId, Comment comment);

    Mono<Void> deleteByIds(Set<UUID> commentIds);

    Mono<Void> deleteByEntityId(EntityType entityType, UUID entityId);

    Mono<Void> deleteByEntityIds(EntityType entityType, Set<UUID> entityIds);
}

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
class CommentDAOImpl implements CommentDAO {

    private static final String INSERT_COMMENT = """
            INSERT INTO comments(
                id,
                entity_id,
                project_id,
                workspace_id,
                text,
                created_by,
                last_updated_by
            )
            VALUES
            (
                 :id,
                 :entity_id,
                 :project_id,
                 :workspace_id,
                 :text,
                 :user_name,
                 :user_name
            )
            ;
            """;

    private static final String SELECT_COMMENT_BY_ID = """
            SELECT
                *
            FROM comments
            WHERE workspace_id = :workspace_id
            <if(entity_id)> AND entity_id = :entity_id <endif>
            AND id = :id
            ORDER BY id DESC, last_updated_at DESC
            LIMIT 1 BY id
            ;
            """;

    private static final String UPDATE = """
            INSERT INTO comments (
            	id, entity_id, entity_type, project_id, workspace_id, text, created_at, created_by, last_updated_by
            ) SELECT
            	id,
            	entity_id,
            	entity_type,
            	project_id,
            	workspace_id,
            	:text as text,
            	created_at,
            	created_by,
                :user_name as last_updated_by
            FROM comments
            WHERE id = :id
            AND workspace_id = :workspace_id
            ORDER BY id DESC, last_updated_at DESC
            LIMIT 1
            ;
            """;

    private static final String DELETE_COMMENT_BY_ID = """
            DELETE FROM comments
            WHERE id IN :ids
            AND workspace_id = :workspace_id
            ;
            """;

    private static final String DELETE_COMMENT_BY_ENTITY_IDS = """
            DELETE FROM comments
            WHERE entity_id IN :entity_ids
            AND entity_type = :entity_type
            AND workspace_id = :workspace_id
            ;
            """;

    private final @NonNull TransactionTemplateAsync asyncTemplate;

    @Override
    public Mono<Long> addComment(@NonNull UUID commentId, @NonNull UUID entityId, @NonNull UUID projectId,
            @NonNull Comment comment) {
        return asyncTemplate.nonTransaction(connection -> {

            var statement = connection.createStatement(INSERT_COMMENT);

            bindParameters(commentId, entityId, projectId, comment, statement);

            return makeMonoContextAware(bindUserNameAndWorkspaceContext(statement))
                    .flatMap(result -> Mono.from(result.getRowsUpdated()));
        });
    }

    @Override
    public Mono<Comment> findById(UUID traceId, @NonNull UUID commentId) {
        return asyncTemplate.nonTransaction(connection -> {

            var template = new ST(SELECT_COMMENT_BY_ID);
            if (traceId != null) {
                template.add("entity_id", traceId);
            }

            var statement = connection.createStatement(template.render())
                    .bind("id", commentId);

            if (traceId != null) {
                statement.bind("entity_id", traceId);
            }

            return makeFluxContextAware(bindWorkspaceIdToFlux(statement))
                    .flatMap(CommentResultMapper::mapItem)
                    .singleOrEmpty();
        });
    }

    @Override
    public Mono<Void> updateComment(@NonNull UUID commentId, @NonNull Comment comment) {
        return asyncTemplate.nonTransaction(connection -> {

            var statement = connection.createStatement(UPDATE)
                    .bind("text", comment.text())
                    .bind("id", commentId);

            return makeMonoContextAware(bindUserNameAndWorkspaceContext(statement))
                    .then();
        });
    }

    @Override
    public Mono<Void> deleteByIds(@NonNull Set<UUID> commentIds) {
        return asyncTemplate.nonTransaction(connection -> {

            var statement = connection.createStatement(DELETE_COMMENT_BY_ID)
                    .bind("ids", commentIds);

            return makeMonoContextAware(bindWorkspaceIdToMono(statement))
                    .then();
        });
    }

    @Override
    public Mono<Void> deleteByEntityId(@NonNull EntityType entityType, @NonNull UUID entityId) {
        return deleteByEntityIds(entityType, Set.of(entityId));
    }

    @Override
    public Mono<Void> deleteByEntityIds(@NonNull EntityType entityType, Set<UUID> entityIds) {
        Preconditions.checkArgument(
                CollectionUtils.isNotEmpty(entityIds), "Argument 'entityIds' must not be empty");
        log.info("Deleting comments for entityType '{}', entityIds count '{}'", entityType, entityIds.size());

        return asyncTemplate.nonTransaction(connection -> {

            var statement = connection.createStatement(DELETE_COMMENT_BY_ENTITY_IDS)
                    .bind("entity_ids", entityIds)
                    .bind("entity_type", entityType.getType());

            return makeMonoContextAware(bindWorkspaceIdToMono(statement))
                    .then();
        });
    }

    private void bindParameters(UUID commentId, UUID entityId, UUID projectId, Comment comment, Statement statement) {
        statement.bind("id", commentId)
                .bind("entity_id", entityId)
                .bind("project_id", projectId)
                .bind("text", comment.text());
    }
}

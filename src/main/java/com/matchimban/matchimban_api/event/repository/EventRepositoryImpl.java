package com.matchimban.matchimban_api.event.repository;

import com.matchimban.matchimban_api.event.entity.Event;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepositoryCustom {

    @PersistenceContext
    private final EntityManager em;

    @Override
    public List<Event> findEventPage(Instant cursorCreatedAt, Long cursorId, int size) {
        String baseQuery = """
                SELECT e
                FROM Event e
                WHERE e.isActive = true
                  AND e.isDeleted = false
                """;

        String cursorQuery = """
                  AND (
                        e.createdAt < :cursorCreatedAt
                        OR (e.createdAt = :cursorCreatedAt AND e.id < :cursorId)
                  )
                """;

        String orderQuery = """
                ORDER BY e.createdAt DESC, e.id DESC
                """;

        var jpql = new StringBuilder(baseQuery);
        boolean hasCursor = cursorCreatedAt != null && cursorId != null;
        if (hasCursor) {
            jpql.append(cursorQuery);
        }
        jpql.append(orderQuery);

        var query = em.createQuery(jpql.toString(), Event.class)
                .setMaxResults(size);

        if (hasCursor) {
            query.setParameter("cursorCreatedAt", cursorCreatedAt);
            query.setParameter("cursorId", cursorId);
        }

        return query.getResultList();
    }
}
package one.june.leave_management.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Generic domain event representing something that happened to an entity.
 * This decouples event publishers from consumers and allows for flexible event routing.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityEvent {

    /**
     * The type of event that occurred
     */
    private EventType eventType;

    /**
     * The type of entity this event relates to
     */
    private EntityType entityType;

    /**
     * The unique identifier of the entity
     */
    private UUID entityId;

    /**
     * Optional metadata associated with the event.
     * Can contain additional context needed for processing.
     */
    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    /**
     * Create a new entity event without metadata.
     */
    public static EntityEvent of(EventType eventType, EntityType entityType, UUID entityId) {
        return EntityEvent.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .build();
    }

    /**
     * Create a new entity event with metadata.
     */
    public static EntityEvent of(EventType eventType, EntityType entityType, UUID entityId, Map<String, Object> metadata) {
        return EntityEvent.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(metadata)
                .build();
    }
}

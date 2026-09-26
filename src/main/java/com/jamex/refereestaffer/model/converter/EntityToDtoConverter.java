package com.jamex.refereestaffer.model.converter;

import java.util.Collection;
import java.util.stream.StreamSupport;

/**
 * Entity → dto half of a converter. This direction never needs extra context — an entity
 * already holds everything its dto exposes — so every converter can implement it and
 * inherit the bulk mapping below.
 *
 * @see DtoToEntityConverter for the other half, which is not universally implementable
 */
public interface EntityToDtoConverter<E, D> {

    D convertFromEntity(E entity);

    default Collection<D> convertFromEntities(final Iterable<E> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::convertFromEntity)
                .toList();
    }
}

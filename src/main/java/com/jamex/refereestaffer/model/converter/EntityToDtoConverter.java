package com.jamex.refereestaffer.model.converter;

import java.util.Collection;
import java.util.stream.StreamSupport;

/**
 * Entity → dto half of a converter. This direction needs no resolved-id context — an entity
 * already holds the ids its dto exposes — so every converter can implement it and inherit the
 * bulk mapping below. It does still need whatever context the entity's own associations need:
 * with {@code spring.jpa.open-in-view: false}, implementations that dereference an association
 * ({@code VacationConverter} reads {@code getReferee()}, {@code MatchConverter} reads
 * {@code getHome()}) work only because no association in {@code model/entity} declares a
 * {@code FetchType} and {@code @ManyToOne}/{@code @OneToOne} default to eager.
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

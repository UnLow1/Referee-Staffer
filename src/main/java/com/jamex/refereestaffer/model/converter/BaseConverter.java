package com.jamex.refereestaffer.model.converter;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface BaseConverter<E, D> {
    D convertFromEntity(E entity);

    E convertFromDto(D dto);

    default Collection<D> convertFromEntities(final Iterable<E> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::convertFromEntity)
                .collect(Collectors.toList());
    }

    default Collection<E> convertFromDtos(final Iterable<D> dtos) {
        return StreamSupport.stream(dtos.spliterator(), false)
                .map(this::convertFromDto)
                .collect(Collectors.toList());
    }
}

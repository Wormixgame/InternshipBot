package tech.reliab.kaiten.util;

import tech.reliab.kaiten.Entity.Entity;

@FunctionalInterface
public interface DatabaseFilter {
    <T extends Entity> boolean apply(T entity);
}

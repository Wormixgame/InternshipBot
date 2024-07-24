package tech.reliab.kaiten.services;

import tech.reliab.kaiten.Entity.Entity;

public interface GetCourseService {
    <T extends Entity> void signal(Class<T> entityClass, Long id);
}

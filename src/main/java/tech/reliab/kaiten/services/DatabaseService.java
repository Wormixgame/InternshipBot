package tech.reliab.kaiten.services;

import tech.reliab.kaiten.Entity.Entity;

import java.util.List;
import java.util.Map;

public interface DatabaseService {
    <T extends Entity> void create(T entity);
    <T extends Entity> void update(Class<T> entityClass, Map<String, Object> fields, Long id);
    <T extends Entity> void delete(Class<T> entityClass, Long id);
    <T extends Entity> List<T> getEntities(Class<T> entityClass);}

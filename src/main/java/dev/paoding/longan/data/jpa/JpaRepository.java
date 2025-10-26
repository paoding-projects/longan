package dev.paoding.longan.data.jpa;

import dev.paoding.longan.data.Pageable;

import java.util.List;
import java.util.Optional;

public interface JpaRepository<T, ID> {

    long generateId();

    T get(ID id);

    Optional<T> getOptional(ID id);

    Optional<T> getOptional(Example<T> example);

    List<T> find(Pageable pageable);

    List<T> find(Example<T> example);

    List<T> find(Example<T> example, Pageable pageable);

    List<T> find(List<ID> idList);

    List<T> findAll();

    long count();

    long count(Example<T> example);

    boolean exists(Example<T> example);

    boolean exists(ID id);

    boolean exists(T source, Object target);

    boolean exists(T source, Object target, String role);

    T save(T entity);

    List<T> save(List<T> entityList);

    int saveOrUpdate(T entity);

    int deleteById(ID id);

    int deleteAll();

    int delete(List<T> entityList);

    int deleteById(List<ID> idList);

    int increase(ID id, Object... objects);

    int update(T entity);

    int update(T entity, boolean strict);

    int update(List<T> entityList);

    int update(List<ID> idList, T entity);

    int join(T source, Object target);

    int split(T source, Object target);

    int split(T source, Class<?> type);

    int split(Class<?> source, Class<?> target);

    int join(T source, Object target, String role);

    int split(T source, Object target, String role);

    int split(T source, Class<?> type, String role);

    int split(Class<?> source, Class<?> target,String role);

//    Query<T> query(String sql);

//    <D> Query<D> query(String sql, Class<D> requiredType);

//    List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap);

//    <D> List<D> queryForList(String sql, Map<String, ?> paramMap, Class<D> elementType);

//    Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap);

//    <D> D queryForObject(String sql, Map<String, ?> paramMap, Class<D> requiredType);
}

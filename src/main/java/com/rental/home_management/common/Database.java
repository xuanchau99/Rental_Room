package com.rental.home_management.common;

import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class Database {
  @PersistenceContext private EntityManager em;

  public EntityManager em() {
    return em;
  }

  public <T> T get(Class<T> type, Long id) {
    T value = em.find(type, id);
    if (value == null) throw BusinessException.missing(type.getSimpleName() + " not found");
    return value;
  }

  public <T> T lock(Class<T> type, Long id) {
    T value = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
    if (value == null) throw BusinessException.missing(type.getSimpleName() + " not found");
    return value;
  }

  public <T> T save(T value) {
    em.persist(value);
    return value;
  }

  public <T> List<T> query(String jpql, Class<T> type, Object... args) {
    var q = em.createQuery(jpql, type);
    for (int i = 0; i < args.length; i += 2) q.setParameter((String) args[i], args[i + 1]);
    return q.getResultList();
  }

  public <T> Optional<T> first(String jpql, Class<T> type, Object... args) {
    var q = em.createQuery(jpql, type);
    for (int i = 0; i < args.length; i += 2) q.setParameter((String) args[i], args[i + 1]);
    return q.setMaxResults(1).getResultStream().findFirst();
  }

  public <T> List<T> page(String jpql, Class<T> type, int page, int size, Object... args) {
    if (page < 0 || size < 1 || size > 100)
      throw BusinessException.invalid("page >= 0 and size 1..100 required");
    var q = em.createQuery(jpql, type);
    for (int i = 0; i < args.length; i += 2) q.setParameter((String) args[i], args[i + 1]);
    return q.setFirstResult(Math.multiplyExact(page, size)).setMaxResults(size).getResultList();
  }
}

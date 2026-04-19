package com.school.management.domain.shared;

import com.school.management.domain.shared.event.DomainEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for aggregate roots in DDD.
 * Aggregate roots are the entry points to aggregates and manage domain events.
 *
 * @param <ID> the type of the aggregate identifier
 */
public abstract class AggregateRoot<ID extends Serializable> implements Entity<ID> {

    protected ID id;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Long version;

    @Override
    public ID getId() {
        return id;
    }

    /**
     * ⚠️ 该方法仅用于 ORM 反射赋值 / 仓储层在新建保存时设置数据库生成的 ID.
     *
     * 业务代码不应调用该方法修改聚合根 ID — 聚合的身份 (identity) 是不可变的,
     * 在 aggregate 创建时就已确定,变更会破坏 equals/hashCode/ 领域事件一致性.
     *
     * @see #getId()
     */
    public void setId(ID id) {
        this.id = id;
    }

    /**
     * Registers a domain event to be published when the aggregate is saved.
     *
     * @param event the domain event to register
     */
    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /**
     * Returns all domain events registered by this aggregate.
     *
     * @return unmodifiable list of domain events
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Clears all registered domain events.
     * Should be called after events are published.
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    /**
     * Returns the version for optimistic locking.
     *
     * @return the version number
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the version for optimistic locking.
     *
     * @param version the version number
     */
    public void setVersion(Long version) {
        this.version = version;
    }
}

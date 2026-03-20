package com.school.management.domain.organization.event;

import com.school.management.domain.organization.model.Position;
import com.school.management.domain.shared.model.valueobject.FieldChange;
import com.school.management.domain.shared.event.BaseDomainEvent;

import java.util.List;

public class PositionUpdatedEvent extends BaseDomainEvent {

    private final Long positionId;
    private final String positionName;
    private final List<FieldChange> changes;

    public PositionUpdatedEvent(Position position, List<FieldChange> changes) {
        super("Position", position.getId());
        this.positionId = position.getId();
        this.positionName = position.getPositionName();
        this.changes = changes;
    }

    public Long getPositionId() { return positionId; }
    public String getPositionName() { return positionName; }
    public List<FieldChange> getChanges() { return changes; }
}

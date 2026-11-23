package com.smartasset.sentinel.model;

public enum WorkOrderStatus {
    OPEN, IN_PROGRESS, COMPLETED, CANCELLED;

    public boolean isActive() {
        return this == OPEN || this == IN_PROGRESS;
    }

    public boolean canTransitionTo(WorkOrderStatus target) {
        return switch (this) {
            case OPEN -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}

package com.xcappstore.admin.operationlog.dto;

import java.util.List;

public class OperationLogStatsResponse {
    private final List<StatItem> byAction;
    private final List<StatItem> byResourceType;
    private final List<StatItem> byDate;

    public OperationLogStatsResponse(List<StatItem> byAction, List<StatItem> byResourceType, List<StatItem> byDate) {
        this.byAction = byAction;
        this.byResourceType = byResourceType;
        this.byDate = byDate;
    }

    public List<StatItem> getByAction() {
        return byAction;
    }

    public List<StatItem> getByResourceType() {
        return byResourceType;
    }

    public List<StatItem> getByDate() {
        return byDate;
    }
}

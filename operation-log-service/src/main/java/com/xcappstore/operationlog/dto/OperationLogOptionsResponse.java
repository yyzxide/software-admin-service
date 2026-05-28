package com.xcappstore.operationlog.dto;

import java.util.List;

public class OperationLogOptionsResponse {
    private final List<String> actions;
    private final List<String> resourceTypes;

    public OperationLogOptionsResponse(List<String> actions, List<String> resourceTypes) {
        this.actions = actions;
        this.resourceTypes = resourceTypes;
    }

    public List<String> getActions() {
        return actions;
    }

    public List<String> getResourceTypes() {
        return resourceTypes;
    }
}

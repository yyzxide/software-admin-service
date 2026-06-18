package com.xcappstore.admin.review.dto;

import java.time.LocalDateTime;

public class ReviewHistoryResponse {
    private Long id;
    private Long taskId;
    private String action;
    private Integer fromStatus;
    private String fromStatusText;
    private Integer toStatus;
    private String toStatusText;
    private Long operatorId;
    private String comment;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getFromStatus() { return fromStatus; }
    public void setFromStatus(Integer fromStatus) { this.fromStatus = fromStatus; }
    public String getFromStatusText() { return fromStatusText; }
    public void setFromStatusText(String fromStatusText) { this.fromStatusText = fromStatusText; }
    public Integer getToStatus() { return toStatus; }
    public void setToStatus(Integer toStatus) { this.toStatus = toStatus; }
    public String getToStatusText() { return toStatusText; }
    public void setToStatusText(String toStatusText) { this.toStatusText = toStatusText; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

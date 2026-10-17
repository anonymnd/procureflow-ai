package com.project.app.procurement.api;

import java.util.List;

public final class WorkspaceViews {
    private WorkspaceViews() { }
    public record WorkspaceRowView(long id,String title,String status,String counterparty) { }
    public record WorkspaceStatusCountView(String status,long count) { }
    public record WorkspacePageView(String kind,int page,int size,long total,List<WorkspaceRowView> rows,List<WorkspaceStatusCountView> statusCounts) { }
}

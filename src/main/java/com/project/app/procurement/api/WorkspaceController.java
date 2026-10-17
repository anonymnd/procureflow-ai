package com.project.app.procurement.api;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.WorkspaceViews.WorkspacePageView;
import com.project.app.procurement.service.WorkspaceService;
@RestController @RequestMapping("/api/procurement/workspace")
public class WorkspaceController {
    private final WorkspaceService workspace;
    public WorkspaceController(WorkspaceService workspace){this.workspace=workspace;}
    @GetMapping public WorkspacePageView page(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam String kind,@RequestParam(required=false) Integer page,@RequestParam(required=false) Integer size,
            @RequestParam(required=false) String q,@RequestParam(required=false) String status){
        return workspace.workspace(principal,kind,page,size,q,status);
    }
}

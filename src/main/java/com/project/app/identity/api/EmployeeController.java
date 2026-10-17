package com.project.app.identity.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.identity.api.IdentityRequests.EmployeeAccess;
import com.project.app.identity.api.IdentityRequests.InviteEmployee;
import com.project.app.identity.api.IdentityViews.EmployeeView;
import com.project.app.identity.api.IdentityViews.Message;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.identity.service.EmployeeAccessService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeAccessService employees;
    public EmployeeController(EmployeeAccessService employees) { this.employees = employees; }

    @GetMapping
    public List<EmployeeView> list(@AuthenticationPrincipal AccountPrincipal administrator) {
        return employees.list(administrator);
    }

    @GetMapping("/{id}")
    public EmployeeView get(@AuthenticationPrincipal AccountPrincipal administrator, @PathVariable Long id) {
        return employees.get(administrator, id);
    }

    @PostMapping("/invitations")
    public ResponseEntity<Message> invite(@AuthenticationPrincipal AccountPrincipal administrator,
            @Valid @RequestBody InviteEmployee request) {
        employees.invite(administrator, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Message("Invitation sent."));
    }

    @PutMapping("/{id}/access")
    public EmployeeView updateAccess(@AuthenticationPrincipal AccountPrincipal administrator, @PathVariable Long id,
            @Valid @RequestBody EmployeeAccess request) {
        return employees.updateAccess(administrator, id, request);
    }
}

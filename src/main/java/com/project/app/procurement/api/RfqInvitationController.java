package com.project.app.procurement.api;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.ProcurementViews.InvitationView;
import com.project.app.procurement.service.RfqInvitationService;

@RestController
@RequestMapping("/api/procurement/invitations")
public class RfqInvitationController {
    private final RfqInvitationService invitations;
    public RfqInvitationController(RfqInvitationService invitations) { this.invitations = invitations; }

    @GetMapping
    public List<InvitationView> list(@AuthenticationPrincipal AccountPrincipal supplier) {
        return invitations.list(supplier);
    }

    @GetMapping("/{id}")
    public InvitationView get(@AuthenticationPrincipal AccountPrincipal supplier, @PathVariable Long id) {
        return invitations.get(supplier, id);
    }
}

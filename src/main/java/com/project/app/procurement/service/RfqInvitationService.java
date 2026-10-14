package com.project.app.procurement.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.ProcurementViews.InvitationView;
import com.project.app.procurement.model.RfqInvitation;
import com.project.app.procurement.repository.RfqInvitationRepository;
import com.project.app.procurement.repository.RfqItemRepository;

@Service
public class RfqInvitationService {
    private final RfqInvitationRepository invitations;
    private final RfqItemRepository items;

    public RfqInvitationService(RfqInvitationRepository invitations, RfqItemRepository items) {
        this.invitations = invitations;
        this.items = items;
    }

    @Transactional(readOnly = true)
    public List<InvitationView> list(AccountPrincipal supplier) {
        return invitations.findBySupplierOrganizationIdOrderByIdDesc(supplier.getOrganizationId()).stream()
                .map(invitation -> InvitationView.from(invitation, items.findByRfqIdOrderByIdAsc(invitation.getRfq().getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public InvitationView get(AccountPrincipal supplier, Long invitationId) {
        RfqInvitation invitation = invitations.findByIdAndSupplierOrganizationId(invitationId, supplier.getOrganizationId())
                .orElseThrow(ProcurementException::notFound);
        return InvitationView.from(invitation, items.findByRfqIdOrderByIdAsc(invitation.getRfq().getId()));
    }
}

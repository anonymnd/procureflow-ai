package com.project.app.procurement.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.app.identity.model.Organization;
import com.project.app.identity.repository.AccountRepository;
import com.project.app.identity.repository.OrganizationRepository;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.ProcurementRequests.CreateContact;
import com.project.app.procurement.api.ProcurementViews.ContactView;
import com.project.app.procurement.model.SupplierContact;
import com.project.app.procurement.repository.SupplierContactRepository;

@Service
public class SupplierContactService {
    private final SupplierContactRepository contacts;
    private final OrganizationRepository organizations;
    private final AccountRepository accounts;

    public SupplierContactService(SupplierContactRepository contacts, OrganizationRepository organizations,
            AccountRepository accounts) {
        this.contacts = contacts;
        this.organizations = organizations;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<ContactView> list(AccountPrincipal buyer) {
        return contacts.findByBuyerOrganizationIdOrderByIdDesc(buyer.getOrganizationId()).stream()
                .map(ContactView::from).toList();
    }

    @Transactional
    public ContactView create(AccountPrincipal buyer, CreateContact request) {
        if (request.supplierOrganizationId().equals(buyer.getOrganizationId())) {
            throw ProcurementException.badRequest("Cannot add your own organization as a contact.");
        }
        if (!accounts.existsByOrganizationIdAndSupplierGrantTrueAndActiveTrueAndEmailVerifiedTrue(request.supplierOrganizationId())) {
            throw ProcurementException.badRequest("That organization has no active supplier-granted employee.");
        }
        Organization buyerOrganization = organizations.getReferenceById(buyer.getOrganizationId());
        Organization supplierOrganization = organizations.findById(request.supplierOrganizationId())
                .orElseThrow(() -> ProcurementException.badRequest("Unknown supplier organization."));
        SupplierContact contact = new SupplierContact(buyerOrganization, supplierOrganization, request.note());
        return ContactView.from(contacts.save(contact));
    }

    @Transactional
    public void remove(AccountPrincipal buyer, Long contactId) {
        SupplierContact contact = contacts.findByIdAndBuyerOrganizationId(contactId, buyer.getOrganizationId())
                .orElseThrow(ProcurementException::notFound);
        contacts.delete(contact);
    }
}

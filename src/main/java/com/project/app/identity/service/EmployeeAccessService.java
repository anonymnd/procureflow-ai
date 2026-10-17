package com.project.app.identity.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.app.identity.api.IdentityRequests.EmployeeAccess;
import com.project.app.identity.api.IdentityViews.EmployeeView;
import com.project.app.identity.model.Account;
import com.project.app.identity.repository.AccountRepository;
import com.project.app.identity.repository.OrganizationRepository;
import com.project.app.identity.security.AccountPrincipal;

@Service
public class EmployeeAccessService {
    private final AccountRepository accounts;
    private final OrganizationRepository organizations;
    private final IdentityService identity;
    private final SessionRevocationService sessions;

    public EmployeeAccessService(AccountRepository accounts, OrganizationRepository organizations,
            IdentityService identity, SessionRevocationService sessions) {
        this.accounts = accounts;
        this.organizations = organizations;
        this.identity = identity;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public List<EmployeeView> list(AccountPrincipal administrator) {
        return accounts.findByOrganizationIdOrderByFullNameAscIdAsc(administrator.getOrganizationId())
                .stream().map(EmployeeView::from).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeView get(AccountPrincipal administrator, Long id) {
        return accounts.findByIdAndOrganizationId(id, administrator.getOrganizationId())
                .map(EmployeeView::from).orElseThrow(IdentityException::notFound);
    }

    @Transactional
    public void invite(AccountPrincipal administrator, com.project.app.identity.api.IdentityRequests.InviteEmployee request) {
        identity.invite(administrator, request);
    }

    @Transactional
    public EmployeeView updateAccess(AccountPrincipal administrator, Long id, EmployeeAccess update) {
        Long organizationId = administrator.getOrganizationId();
        organizations.findLockedById(organizationId).orElseThrow(IdentityException::notFound);
        Account target = accounts.findLockedInOrganization(id, organizationId).orElseThrow(IdentityException::notFound);
        if (target.isOrganizationAdmin() && target.isActive() && target.isEmailVerified()
                && (!update.organizationAdmin() || !update.active())
                && accounts.countByOrganizationIdAndOrganizationAdminTrueAndActiveTrueAndEmailVerifiedTrue(organizationId) <= 1) {
            throw IdentityException.conflict("The organization must retain an active, verified administrator.");
        }
        boolean changed = target.isOrganizationAdmin() != update.organizationAdmin()
                || target.isBuyerGrant() != update.buyerGrant()
                || target.isSupplierGrant() != update.supplierGrant()
                || target.isActive() != update.active();
        target.setOrganizationAdmin(update.organizationAdmin());
        target.setBuyerGrant(update.buyerGrant());
        target.setSupplierGrant(update.supplierGrant());
        target.setActive(update.active());
        if (changed) target.invalidateSessions();
        Account saved = accounts.saveAndFlush(target);
        if (changed) sessions.revokeAll(saved);
        return EmployeeView.from(saved);
    }
}

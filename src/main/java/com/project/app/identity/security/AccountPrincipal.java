package com.project.app.identity.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.userdetails.UserDetails;

import com.project.app.identity.model.Account;

public final class AccountPrincipal implements UserDetails, CredentialsContainer {
    private final Long accountId;
    private final Long organizationId;
    private final String email;
    private final String fullName;
    private final long authVersion;
    private final boolean organizationAdmin;
    private final boolean buyerGrant;
    private final boolean supplierGrant;
    private String passwordHash;

    public AccountPrincipal(Account account) {
        accountId = account.getId();
        organizationId = account.getOrganization().getId();
        email = account.getEmail();
        fullName = account.getFullName();
        authVersion = account.getAuthVersion();
        organizationAdmin = account.isOrganizationAdmin();
        buyerGrant = account.isBuyerGrant();
        supplierGrant = account.isSupplierGrant();
        passwordHash = account.getPasswordHash();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> result = new ArrayList<>();
        if (organizationAdmin) result.add(new SimpleGrantedAuthority("ROLE_ORG_ADMIN"));
        if (buyerGrant) result.add(new SimpleGrantedAuthority("ROLE_BUYER"));
        if (supplierGrant) result.add(new SimpleGrantedAuthority("ROLE_SUPPLIER"));
        return result;
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return true; }
    @Override public void eraseCredentials() { passwordHash = null; }

    public Long getAccountId() { return accountId; }
    public Long getOrganizationId() { return organizationId; }
    public String getFullName() { return fullName; }
    public long getAuthVersion() { return authVersion; }
    public boolean isOrganizationAdmin() { return organizationAdmin; }
    public boolean isBuyerGrant() { return buyerGrant; }
    public boolean isSupplierGrant() { return supplierGrant; }

    @Override public boolean equals(Object other) {
        return other instanceof AccountPrincipal principal && accountId.equals(principal.accountId);
    }
    @Override public int hashCode() { return accountId.hashCode(); }
}

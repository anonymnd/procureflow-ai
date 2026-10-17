package com.project.app.identity.api;

import java.time.Instant;

import com.project.app.identity.model.Account;

public final class IdentityViews {
    private IdentityViews() { }
    public record AccountView(Long id, String fullName, String email, Long organizationId, String organizationName,
            boolean organizationAdmin, boolean buyerGrant, boolean supplierGrant, boolean active, boolean emailVerified) {
        public static AccountView from(Account a) {
            return new AccountView(a.getId(), a.getFullName(), a.getEmail(), a.getOrganization().getId(),
                    a.getOrganization().getDisplayName(), a.isOrganizationAdmin(), a.isBuyerGrant(),
                    a.isSupplierGrant(), a.isActive(), a.isEmailVerified());
        }
    }
    public record EmployeeView(Long id, String fullName, String email, boolean organizationAdmin,
            boolean buyerGrant, boolean supplierGrant, boolean active, boolean emailVerified, Instant createdAt) {
        public static EmployeeView from(Account a) {
            return new EmployeeView(a.getId(), a.getFullName(), a.getEmail(), a.isOrganizationAdmin(),
                    a.isBuyerGrant(), a.isSupplierGrant(), a.isActive(), a.isEmailVerified(), a.getCreatedAt());
        }
    }
    public record Message(String message) { }
    public record Csrf(String token) { }
}

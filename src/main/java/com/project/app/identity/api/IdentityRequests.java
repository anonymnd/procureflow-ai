package com.project.app.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class IdentityRequests {
    private IdentityRequests() { }
    public record Register(@NotBlank @Size(max = 160) String organizationName, @NotBlank @Size(max = 160) String fullName,
            @NotBlank @Email @Size(max = 320) String email, @NotNull String password) { }
    public record Login(@NotBlank @Email @Size(max = 320) String email, @NotNull String password) { }
    public record UpdateProfile(@NotBlank @Size(max = 160) String fullName) {
        public UpdateProfile { fullName = fullName == null ? null : fullName.strip(); }
    }
    public record UpdateOrganization(@NotBlank @Size(max = 160) String displayName) {
        public UpdateOrganization { displayName = displayName == null ? null : displayName.strip(); }
    }
    public record Token(@NotBlank String token) { }
    public record AcceptInvitation(@NotBlank String token, @NotBlank @Size(max = 160) String fullName, @NotNull String password) { }
    public record InviteEmployee(@NotBlank @Email @Size(max = 320) String email) { }
    public record EmployeeAccess(@NotNull Boolean organizationAdmin, @NotNull Boolean buyerGrant,
            @NotNull Boolean supplierGrant, @NotNull Boolean active) { }
    public record ChangePassword(@NotNull String currentPassword, @NotNull String newPassword) { }
}

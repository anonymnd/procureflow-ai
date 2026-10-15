package com.project.app.identity.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.app.identity.api.IdentityRequests.AcceptInvitation;
import com.project.app.identity.api.IdentityRequests.ChangePassword;
import com.project.app.identity.api.IdentityRequests.InviteEmployee;
import com.project.app.identity.api.IdentityRequests.Register;
import com.project.app.identity.model.Account;
import com.project.app.identity.model.IdentityToken;
import com.project.app.identity.model.Organization;
import com.project.app.identity.repository.AccountRepository;
import com.project.app.identity.repository.IdentityTokenRepository;
import com.project.app.identity.repository.OrganizationRepository;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.identity.api.IdentityViews.AccountView;

@Service
public class IdentityService {
    private final OrganizationRepository organizations;
    private final AccountRepository accounts;
    private final IdentityTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenValues tokenValues;
    private final IdentityEmailService email;
    private final Clock clock;
    private final SessionRevocationService sessionRevocation;

    public IdentityService(OrganizationRepository organizations, AccountRepository accounts,
            IdentityTokenRepository tokens, PasswordEncoder passwordEncoder, TokenValues tokenValues,
            IdentityEmailService email, Clock clock, SessionRevocationService sessionRevocation) {
        this.organizations = organizations;
        this.accounts = accounts;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenValues = tokenValues;
        this.email = email;
        this.clock = clock;
        this.sessionRevocation = sessionRevocation;
    }

    @Transactional
    public void register(Register request) {
        requirePassword(request.password());
        String normalizedEmail = EmailAddress.normalize(request.email());
        if (accounts.findByEmail(normalizedEmail).isPresent()) {
            throw IdentityException.conflict("Registration could not be completed.");
        }
        Organization organization = organizations.saveAndFlush(new Organization(request.organizationName().strip()));
        Account admin = accounts.saveAndFlush(new Account(request.fullName().strip(), normalizedEmail,
                passwordEncoder.encode(request.password()), organization, true, false));
        issueVerification(admin);
    }

    @Transactional
    public void invite(AccountPrincipal inviter, InviteEmployee request) {
        Organization organization = organizations.findLockedById(inviter.getOrganizationId())
                .orElseThrow(IdentityException::notFound);
        String targetEmail = EmailAddress.normalize(request.email());
        if (accounts.findByEmail(targetEmail).isPresent()) {
            throw IdentityException.conflict("The employee invitation could not be created.");
        }
        Instant now = clock.instant();
        tokens.findUnusedInvitations(IdentityToken.Purpose.INVITATION, organization.getId(), targetEmail)
                .forEach(token -> token.consume(now));
        String rawToken = tokenValues.newToken();
        tokens.saveAndFlush(new IdentityToken(tokenValues.hash(rawToken), IdentityToken.Purpose.INVITATION,
                targetEmail, organization, null, now.plus(Duration.ofDays(7))));
        email.sendInvitation(targetEmail, rawToken);
    }

    @Transactional
    public void acceptInvitation(AcceptInvitation request) {
        requirePassword(request.password());
        Instant now = clock.instant();
        IdentityToken invitation = tokens.findUnusedForUpdate(tokenValues.hash(request.token()), IdentityToken.Purpose.INVITATION)
                .filter(token -> token.getExpiresAt().isAfter(now))
                .orElseThrow(IdentityService::invalidToken);
        if (accounts.findByEmail(invitation.getEmail()).isPresent()) throw invalidToken();
        invitation.consume(now);
        Account employee = accounts.saveAndFlush(new Account(request.fullName().strip(), invitation.getEmail(),
                passwordEncoder.encode(request.password()), invitation.getOrganization(), false, false));
        issueVerification(employee);
    }

    @Transactional
    public void verify(String rawToken) {
        Instant now = clock.instant();
        IdentityToken token = tokens.findUnusedForUpdate(tokenValues.hash(rawToken), IdentityToken.Purpose.VERIFICATION)
                .filter(candidate -> candidate.getExpiresAt().isAfter(now))
                .orElseThrow(IdentityService::invalidToken);
        Account account = token.getAccount();
        if (account == null || !account.isActive() || !account.getEmail().equals(token.getEmail())) throw invalidToken();
        token.consume(now);
        account.setEmailVerified(true);
        tokens.findUnusedForAccount(IdentityToken.Purpose.VERIFICATION, account.getId()).forEach(other -> other.consume(now));
        accounts.save(account);
    }

    @Transactional(readOnly = true)
    public AccountView current(AccountPrincipal principal) {
        Account account = accounts.findByIdAndOrganizationId(principal.getAccountId(), principal.getOrganizationId())
                .orElseThrow(IdentityException::notFound);
        return AccountView.from(account);
    }

    @Transactional
    public AccountView updateProfile(AccountPrincipal principal, String fullName) {
        Account account = eligibleAccount(principal);
        account.setFullName(profileName(fullName));
        return AccountView.from(accounts.saveAndFlush(account));
    }

    @Transactional
    public AccountView updateOrganization(AccountPrincipal principal, String displayName) {
        // Match employee-access lock order: organization before account.
        Organization organization = organizations.findLockedById(principal.getOrganizationId())
                .orElseThrow(IdentityException::notFound);
        Account account = eligibleAccount(principal);
        if (!account.isOrganizationAdmin()) {
            throw new IdentityException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Organization administrator access is required.");
        }
        organization.setDisplayName(profileName(displayName));
        organizations.saveAndFlush(organization);
        return AccountView.from(account);
    }

    private Account eligibleAccount(AccountPrincipal principal) {
        Account account = accounts.findLockedInOrganization(principal.getAccountId(), principal.getOrganizationId())
                .orElseThrow(IdentityException::notFound);
        if (!account.isActive() || !account.isEmailVerified() || account.getAuthVersion() != principal.getAuthVersion()) {
            throw new IdentityException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Please sign in with an active verified account.");
        }
        return account;
    }

    private static String profileName(String value) {
        if (value == null || value.strip().isEmpty() || value.strip().length() > 160) {
            throw IdentityException.badRequest("Name must contain 1 to 160 characters.");
        }
        return value.strip();
    }

    @Transactional
    public void changePassword(AccountPrincipal principal, ChangePassword change) {
        requirePassword(change.newPassword());
        Account account = accounts.findLockedInOrganization(principal.getAccountId(), principal.getOrganizationId())
                .orElseThrow(IdentityException::notFound);
        if (!passwordEncoder.matches(change.currentPassword(), account.getPasswordHash())) {
            throw IdentityException.badRequest("Current password is incorrect.");
        }
        account.setPasswordHash(passwordEncoder.encode(change.newPassword()));
        account.invalidateSessions();
        Account saved = accounts.saveAndFlush(account);
        sessionRevocation.revokeAll(saved);
    }

    private void issueVerification(Account account) {
        Instant now = clock.instant();
        tokens.findUnusedForAccount(IdentityToken.Purpose.VERIFICATION, account.getId()).forEach(token -> token.consume(now));
        String rawToken = tokenValues.newToken();
        tokens.saveAndFlush(new IdentityToken(tokenValues.hash(rawToken), IdentityToken.Purpose.VERIFICATION,
                account.getEmail(), null, account, now.plus(Duration.ofHours(24))));
        email.sendVerification(account.getEmail(), rawToken);
    }

    public void requirePassword(String password) {
        if (!PasswordPolicy.accepts(password)) {
            throw IdentityException.badRequest("Password must contain 12 to 128 Unicode code points.");
        }
    }

    private static IdentityException invalidToken() {
        return new IdentityException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "This link is invalid or expired.");
    }
}

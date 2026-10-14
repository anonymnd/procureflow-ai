package com.project.app.identity.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.identity.api.IdentityRequests.AcceptInvitation;
import com.project.app.identity.api.IdentityRequests.ChangePassword;
import com.project.app.identity.api.IdentityRequests.Login;
import com.project.app.identity.api.IdentityRequests.Register;
import com.project.app.identity.api.IdentityRequests.Token;
import com.project.app.identity.api.IdentityViews.Csrf;
import com.project.app.identity.api.IdentityViews.Message;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.identity.security.SessionLifetimeFilter;
import com.project.app.identity.service.IdentityService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/auth")
public class IdentityController {
    private final IdentityService identity;
    private final AuthenticationManager authenticationManager;
    private final SessionRegistry sessions;
    private final HttpSessionSecurityContextRepository securityContexts = new HttpSessionSecurityContextRepository();

    public IdentityController(IdentityService identity, AuthenticationManager authenticationManager,
            SessionRegistry sessions) {
        this.identity = identity;
        this.authenticationManager = authenticationManager;
        this.sessions = sessions;
    }

    @GetMapping("/csrf")
    public Csrf csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) throw new AuthenticationServiceException("CSRF token was not initialized.");
        return new Csrf(token.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<Message> register(@Valid @RequestBody Register request) {
        identity.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Message("Check your email to verify your account before signing in."));
    }

    @PostMapping("/verify")
    public Message verify(@Valid @RequestBody Token request) {
        identity.verify(request.token());
        return new Message("Email verified. You can now sign in.");
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<Message> acceptInvitation(@Valid @RequestBody AcceptInvitation request) {
        identity.acceptInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Message("Invitation accepted. Check your email to verify the account."));
    }

    @PostMapping("/login")
    public IdentityViews.AccountView login(@Valid @RequestBody Login request, HttpServletRequest servletRequest,
            HttpServletResponse response) {
        final Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        } catch (AuthenticationException failure) {
            throw new IdentityServiceAuthenticationException();
        }
        if (!(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new IdentityServiceAuthenticationException();
        }
        HttpSession session = servletRequest.getSession(true);
        servletRequest.changeSessionId();
        session = servletRequest.getSession(false);
        session.setMaxInactiveInterval(30 * 60);
        session.setAttribute(SessionLifetimeFilter.AUTHENTICATED_AT, System.currentTimeMillis());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContexts.saveContext(context, servletRequest, response);
        sessions.registerNewSession(session.getId(), principal);
        return identity.current(principal);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        SecurityContextHolder.clearContext();
        if (session != null) {
            sessions.removeSessionInformation(session.getId());
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public IdentityViews.AccountView current(@AuthenticationPrincipal AccountPrincipal principal) {
        return identity.current(principal);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ChangePassword request) {
        identity.changePassword(principal, request);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    public static final class IdentityServiceAuthenticationException extends RuntimeException { }
}

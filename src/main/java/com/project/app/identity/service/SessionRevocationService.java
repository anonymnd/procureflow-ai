package com.project.app.identity.service;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import com.project.app.identity.model.Account;
import com.project.app.identity.security.AccountPrincipal;

@Service
public class SessionRevocationService {
    private final SessionRegistry sessions;
    public SessionRevocationService(SessionRegistry sessions) { this.sessions = sessions; }
    public void revokeAll(Account account) {
        for (Object principal : sessions.getAllPrincipals()) {
            if (principal instanceof AccountPrincipal p && p.getAccountId().equals(account.getId())) {
                for (SessionInformation session : sessions.getAllSessions(principal, false)) session.expireNow();
            }
        }
    }
}

CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    organization_admin BOOLEAN NOT NULL DEFAULT FALSE,
    buyer_grant BOOLEAN NOT NULL DEFAULT FALSE,
    supplier_grant BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    auth_version BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT accounts_email_normalized CHECK (email = lower(btrim(email)))
);
CREATE INDEX accounts_organization_id_idx ON accounts(organization_id);

CREATE TABLE identity_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash CHAR(64) NOT NULL UNIQUE,
    purpose VARCHAR(20) NOT NULL CHECK (purpose IN ('VERIFICATION', 'INVITATION')),
    email VARCHAR(320) NOT NULL,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    account_id BIGINT REFERENCES accounts(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CONSTRAINT identity_tokens_target CHECK (
        (purpose = 'VERIFICATION' AND account_id IS NOT NULL AND organization_id IS NULL)
        OR (purpose = 'INVITATION' AND organization_id IS NOT NULL AND account_id IS NULL)
    )
);
CREATE INDEX identity_tokens_pending_email_idx ON identity_tokens(purpose, email, expires_at)
    WHERE consumed_at IS NULL;

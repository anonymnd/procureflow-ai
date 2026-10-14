CREATE TABLE marketplace_profiles (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE RESTRICT,
    description VARCHAR(2000) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE marketplace_offerings (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES marketplace_profiles(id) ON DELETE RESTRICT,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    category VARCHAR(20) NOT NULL CHECK (category IN ('IT_EQUIPMENT', 'OFFICE_SUPPLIES', 'FURNITURE', 'INDUSTRIAL_SUPPLIES', 'OTHER')),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX marketplace_offerings_profile_id_idx ON marketplace_offerings(profile_id);
CREATE INDEX marketplace_offerings_published_idx ON marketplace_offerings(published_at DESC, id DESC) WHERE published;
CREATE INDEX marketplace_offerings_category_idx ON marketplace_offerings(category) WHERE published;

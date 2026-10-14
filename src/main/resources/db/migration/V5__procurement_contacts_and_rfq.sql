CREATE TABLE supplier_contacts (
    id BIGSERIAL PRIMARY KEY,
    buyer_organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    supplier_organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    note VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT supplier_contacts_pair_unique UNIQUE (buyer_organization_id, supplier_organization_id),
    CONSTRAINT supplier_contacts_not_self CHECK (buyer_organization_id <> supplier_organization_id)
);
CREATE INDEX supplier_contacts_buyer_idx ON supplier_contacts(buyer_organization_id);

CREATE TABLE rfqs (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    title VARCHAR(160) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SENT')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX rfqs_organization_idx ON rfqs(organization_id);

CREATE TABLE rfq_items (
    id BIGSERIAL PRIMARY KEY,
    rfq_id BIGINT NOT NULL REFERENCES rfqs(id) ON DELETE RESTRICT,
    description VARCHAR(2000) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 1000000),
    unit VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX rfq_items_rfq_idx ON rfq_items(rfq_id);

CREATE TABLE rfq_invitations (
    id BIGSERIAL PRIMARY KEY,
    rfq_id BIGINT NOT NULL REFERENCES rfqs(id) ON DELETE RESTRICT,
    supplier_organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT rfq_invitations_pair_unique UNIQUE (rfq_id, supplier_organization_id)
);
CREATE INDEX rfq_invitations_supplier_idx ON rfq_invitations(supplier_organization_id);

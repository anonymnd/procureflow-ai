# ProcureFlow

A B2B procurement platform connecting clients and suppliers, from product discovery and requests for quotation to purchase orders and fulfillment tracking.

[Live application](https://procureflow-anonymnd.vercel.app) · [Architecture](docs/architecture.md) · [Setup and deployment](docs/setup.md)

## Features

- Organization accounts, employee invitations and role-based access.
- Public supplier profiles, product discovery and catalog publishing.
- Private supplier contacts and RFQs with requirement items and selected recipients.
- Supplier quotation drafts, submissions and currency-aware comparisons.
- Transactional quotation acceptance and purchase-order snapshots.
- Order tracking, operational queues, dashboards and completed-order analytics.

## Stack

Java 17 · Spring Boot · Spring Security · Spring Data JPA · PostgreSQL · Flyway · Docker. Frontend branches use React, Vite and CSS.

The application uses a feature-oriented modular monolith. Tests cover business rules, authorization, database constraints and important user journeys. See the linked documentation for setup and architectural details.

## Repository structure

`main` contains the consolidated application, architecture/setup documentation and [portfolio assets](docs/portfolio/README.md). It includes changes that may not yet be deployed; repository updates alone do not establish hosted availability.

Two deployment branches are retained because the existing frontend/backend hosting configurations reference them. They should be removed only after those configurations are moved to the intended release process.

The repository contains fictional test/sample data. Production credentials are not published here. AI extraction and supplier scoring are not delivered features.

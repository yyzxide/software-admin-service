# Project Outline

## Project Name

Java Software Admin Service

## Project Positioning

This project is a standalone Java implementation of a software store admin backend for an internal testing stage.

It is not a direct fork of an existing service. The old software store project is only used as a business reference: package distribution, software metadata, categories, tags, versions, package files, and publish/unpublish operations.

The goal is to build a complete Java backend story with real admin-side workflows:

- Software package upload.
- Software metadata management.
- Category and tag management.
- Version and package metadata persistence.
- Publish and unpublish state transitions.
- Admin login and request authorization.
- MySQL transactions, MyBatis dynamic SQL, Redis cache, and local frontend verification.

## Target Users

- Internal operation/admin users.
- Test-stage product or QA users.
- Backend developers verifying software store workflows.

## Architecture

```text
admin-ui
  |
  | HTTP + Bearer token
  v
admin-service
  |-- Spring MVC controllers
  |-- Service layer with validation and transactions
  |-- MyBatis mappers and XML SQL
  |-- Redis cache for category/tag/software detail
  |-- Local package storage with SHA256 calculation
  v
MySQL db_java_software_admin
```

## Main Modules

### Auth

- Local admin login for testing.
- HMAC token generation and verification.
- `Authorization: Bearer` request authorization.
- Gateway-style headers can still be supported later.

### Category

- Category create/update/delete.
- Tree query.
- Status toggle.
- Unique-name validation.
- Delete checks for child categories and related software.
- Redis cache invalidation.

### Tag

- Tag create/update/delete.
- Hot tag query and hot toggle.
- Unique-name validation.
- Delete checks for related software.
- Redis cache invalidation.

### Software

- Admin software list with filters.
- Software detail.
- Multipart package upload.
- SHA256 calculation.
- App/version/package/tag metadata persistence.
- Publish/unpublish state transitions.
- Redis software detail cache.

### Operation Log

Current operation log service is kept as an optional reference module.

The next better direction is to merge operation audit capability into `admin-service` instead of keeping it as the main story.

## Data Model

The standalone database is `db_java_software_admin`.

Main tables:

- `categories`
- `tags`
- `apps`
- `app_versions`
- `app_packages`
- `app_tags`

The schema is maintained by this project:

```text
database/mysql/001_init_admin_schema.sql
```

## Current Scope

Implemented:

- Admin login.
- Category management.
- Tag management.
- Software upload.
- Software list/detail query.
- Software edit.
- Version append with package upload.
- Existing-version package variant append.
- Version/package metadata query.
- Publish/unpublish flow.
- MySQL schema.
- Redis integration points.
- Static admin UI.
- Unit and controller tests.

Not implemented yet:

- Real operation audit write.
- Review workflow.
- Chunk upload.
- SDK download/client API.
- Production-grade deployment and monitoring.

## Design Direction

This project should keep moving as an independent Java backend project.

Reference the old internal software store only at the business level, not at the code or schema level. When implementation details conflict, prefer a clean Java backend design:

- Clear Controller/Service/Mapper layering.
- Independent schema.
- Testable service logic.
- Practical admin UI for verification.
- Stable local and WSL setup.

# Catalog Release Automation

This repository follows the unified Cloud Feeds branch promotion model:

- `CF-<ticket>-<slug>` for work branches
- `test/*` for integration promotion
- `stg/*` for staging promotion
- `main` for production

Promotion is enforced by pull request policy:

- `CF-*` -> `test/*`
- `test/*` -> `stg/*`
- `stg/*` -> `main`

## Workflows

- `pr-policy.yml`
  - Validates that pull requests follow the allowed promotion path.
- `ci.yml`
  - Runs on pushes to `CF-*` and internal pull requests from `CF-*`.
  - Executes Maven tests and a smoke Docker build using test-shaped placeholder URLs.
- `build-test.yml`
  - Runs on pushes to `test/*`.
  - Builds a WAR artifact and a Docker image tagged `test-<sha>` and `test-latest`.
- `build-staging.yml`
  - Runs on pushes to `stg/*`.
  - Builds a WAR artifact and a Docker image tagged `stg-<sha>` and `stg-latest`.
- `release-prod.yml`
  - Runs on pushes to `main`.
  - Builds a WAR artifact and a Docker image tagged `prod-<sha>`, `prod-latest`, and the Maven project version.

## Docker build contract

The Docker build now uses the checked-out repository instead of cloning the repo during image build.

Required build args:

- `SCHEMA_VERSION`
- `CATALOG_VERSION`
- `GITHUB_ACTOR`
- `TOKEN`
- `environment=test|staging|prod`
- `CATALOG_VIP_URL`
- `CATALOG_EXTERNAL_VIP_URL`
- `CATALOG_PREFS_SERVICE_URL`

Config layering:

- `docker/config/common/feedscatalog.xml.tpl`
- `docker/config/env/test|staging|prod/catalog.env`

The final `/etc/feedscatalog/feedscatalog.xml` file is rendered at image build time so shared config does not hardcode environment URLs.

## Required GitHub settings

Configure these outside the repository:

- Branch protections for `main`, `test/*`, and `stg/*`
- GitHub Environments named `test`, `staging`, and `prod`
- Environment secret:
  - `AWS_ACCOUNT_ID`
- Environment variables:
  - `AWS_REGION`
  - `SCHEMA_VERSION`
  - `CATALOG_VIP_URL`
  - `CATALOG_EXTERNAL_VIP_URL`
  - `CATALOG_PREFS_SERVICE_URL`
  - `IMAGE_REPOSITORY` (optional, defaults to `catalog`)
- Workflow permission:
  - `packages: read` so Maven and Docker builds can resolve GitHub Packages dependencies with the default `GITHUB_TOKEN`

## Tag isolation

Staging and production are kept isolated with distinct immutable tags:

- Test: `test-<sha>`
- Staging: `stg-<sha>`
- Production: `prod-<sha>` and `${project.version}`

If stronger isolation is needed, point each GitHub Environment at a different `IMAGE_REPOSITORY`.

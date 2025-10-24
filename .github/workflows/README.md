# GitHub Actions Workflows

This directory contains the GitHub Actions workflows for the project.

## Workflows

### 1. tests.yml
- **Trigger**: Push to master branch or pull requests to master
- **Purpose**: Run all tests (unit and integration) on multiple Tarantool versions
- **Runs on**: Ubuntu latest

### 2. release.yml
- **Trigger**: Creation of tags matching semantic versioning pattern (e.g., `1.4.0`, `2.0.1`)
- **Purpose**: Automatically release to Maven Central and create GitHub Release
- **Runs on**: Ubuntu latest
- **Requires secrets**:
  - `OSSRH_USERNAME` - Sonatype OSSRH username
  - `OSSRH_TOKEN` - Sonatype OSSRH password or token
  - `MAVEN_GPG_PRIVATE_KEY` - GPG private key for signing artifacts
  - `MAVEN_GPG_PASSPHRASE` - Passphrase for the GPG key
  - `TARANTOOL_REGISTRY_PASSWORD` - Password for Tarantool private registry

### 3. snapshot.yml
- **Trigger**: Push to master branch (except commits starting with "Release")
- **Purpose**: Automatically deploy snapshot versions to Maven Central
- **Runs on**: Ubuntu latest
- **Requires secrets**:
  - `OSSRH_USERNAME` - Sonatype OSSRH username
  - `OSSRH_TOKEN` - Sonatype OSSRH password or token
  - `TARANTOOL_REGISTRY_PASSWORD` - Password for Tarantool private registry

## Setup Instructions

To enable automatic releases, you need to configure the following secrets in your GitHub repository settings:

1. Go to Settings → Secrets and variables → Actions
2. Add the following secrets:
   - `OSSRH_USERNAME` - Your Sonatype OSSRH username
   - `OSSRH_TOKEN` - Your Sonatype OSSRH token/password
   - `MAVEN_GPG_PRIVATE_KEY` - Your GPG private key (export with `gpg --export-secret-keys --armor KEY_ID`)
   - `MAVEN_GPG_PASSPHRASE` - Passphrase for your GPG key
   - `TARANTOOL_REGISTRY_PASSWORD` - Password for the Tarantool private registry

## Release Process

### Automatic Release
1. Create and push a new tag with semantic versioning format (e.g., `git tag 1.4.0 && git push origin 1.4.0`)
2. The release workflow will automatically:
   - Build and test the project
   - Sign the artifacts with GPG
   - Deploy to Maven Central
   - Create a GitHub Release

### Automatic Snapshot Deployment
1. Push changes to the master branch
2. The snapshot workflow will automatically:
   - Build and test the project
   - Deploy snapshot version to Maven Central

Note: Commits with messages starting with "Release" are excluded from snapshot deployment to avoid duplicate deployments during the release process.

# SIHSalus REST 3.5.1-sihsalus.2

SIHSalus maintenance build for OpenMRS Core 2.8.9 / Java 21 (javax Servlet).
This is not an official OpenMRS 3.5.1 release.

## Source and scope

- Upstream base: OpenMRS REST 3.5.0, commit `69fa31fc157be0b0835e2101a0b0e480e0da4acb`.
- Backport of [upstream PR #748](https://github.com/openmrs/openmrs-module-webservices.rest/pull/748):
  read CLOB uploads as UTF-8; serve CLOB and delegated form-resource content as
  UTF-8 plain text with `X-Content-Type-Options: nosniff`.
- Backports of [upstream PR #766](https://github.com/openmrs/openmrs-module-webservices.rest/pull/766)
  (`ed382e24b442ff7965bc8a01f9958eb1171b0954`) and
  [PR #770](https://github.com/openmrs/openmrs-module-webservices.rest/pull/770)
  (`53493a5d53a68d941f5611c7b1fb31d003f8f283`): arbitrate class-based
  resource lookup by its declared order and let the session endpoint report an
  expired session without a filter-level 401. Other protected endpoints retain
  their existing authentication behavior. Both upstream regression suites are
  included.
- Module descriptor uses the exact release version, without an SCM suffix.
- The source and regression tests are committed here. No external patch file is applied.
- Authorization tests commit their initial, isolated H2 role fixtures so Core
  2.8.9's independent role-privilege cache transaction can read them. Test-body
  writes still roll back, fixture metadata is removed after that rollback, and
  the existing denied-access assertions are preserved. Runtime authorization
  behavior is unchanged.

## Verification and publication

CI must pass the complete Maven reactor on Java 21 against the upstream Core
2.8.7 baseline and Core 2.8.9. The published OMOD is the exact artifact built and
tested against Core 2.8.9, not a second untested rebuild. External-server REST
integration tests are not part of this reactor; run synthetic form open/save/edit
acceptance in DEV before any clinical deployment.

The release workflow verifies module identity, version, UTF-8/plain-text/nosniff
compiled markers and javax compatibility; it publishes a SHA-256 checksum and
GitHub build attestation. Releases are immutable SIHSalus prereleases pending
deployment acceptance. Never replace an existing release or tag.

Verify a downloaded OMOD with `gh attestation verify FILE --repo
sihsalus/openmrs-module-webservices.rest`, and compare its SHA-256 to the approved
distribution pin. Release publication does not deploy or restart OpenMRS.

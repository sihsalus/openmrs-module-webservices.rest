# SIHSalus REST 3.6.0-sihsalus.1

SIHSalus maintenance build for OpenMRS Core 2.8.10 / Java 21 (javax Servlet).
This is not the unmodified official OpenMRS 3.6.0 release.

## Source and scope

- Upstream base: OpenMRS REST 3.6.0, commit `a57eab44607669b6de0a8ef286558d16210d432e`.
- Includes upstream session-expiry handling (RESTWS-1053), deterministic resource
  selection (RESTWS-1052), and Swagger getter deprecations (RESTWS-1043).
- Retained after reevaluation: backport of [upstream PR #748](https://github.com/openmrs/openmrs-module-webservices.rest/pull/748):
  read CLOB uploads as UTF-8; serve CLOB and delegated form-resource content as
  UTF-8 plain text with `X-Content-Type-Options: nosniff`.
- Module descriptor uses the exact release version, without an SCM suffix.
- The source and regression tests are committed here. No external patch file is applied.
- Authorization tests commit their initial, isolated H2 role fixtures so Core
  2.8.9's independent role-privilege cache transaction can read them. Test-body
  writes still roll back, fixture metadata is removed after that rollback, and
  the existing denied-access assertions are preserved. Runtime authorization
  behavior is unchanged.
- Cache-controller tests retain the isolated transaction and cold-fixture setup
  verified in the earlier backport PR. They explicitly warm the target and a
  control, preserving assertions that selective eviction leaves the control
  cached. These are test-only changes needed for repeatable native validation.
- The password-reset test seeds its activation token with the dedicated DAO
  method. Core 2.8.10 restricts general credential updates; the runtime guard and
  existing password-reset assertions remain intact.

## Verification and publication

CI must pass the complete Maven reactor on Java 21 against the upstream Core
2.8.7 baseline, Core 2.8.9 and Core 2.8.10. The published OMOD is the exact artifact built and
tested against Core 2.8.10, not a second untested rebuild. External-server REST
integration tests are not part of this reactor; run synthetic form open/save/edit
acceptance in DEV before any clinical deployment.

The release workflow verifies module identity, version, UTF-8/plain-text/nosniff
compiled markers and javax compatibility; it publishes a SHA-256 checksum and
GitHub build attestation. Releases are immutable SIHSalus prereleases pending
deployment acceptance. Never replace an existing release or tag.

Verify a downloaded OMOD with `gh attestation verify FILE --repo
sihsalus/openmrs-module-webservices.rest`, and compare its SHA-256 to the approved
distribution pin. Release publication does not deploy or restart OpenMRS.

## Reevaluation of SIHSalus changes

Official REST 3.6.0 still lacks the CLOB UTF-8/plain-text/nosniff backport.
Keep this protection until the supported upstream 3.x release includes it; do
not replace it with the Jakarta-only master branch. The committed role fixtures
affect tests only and preserve denied-access assertions. No runtime permission,
password or session policy is changed by those fixtures.

Publication and deployment remain pending human review. This source update does
not change the distribution pin or publish an OMOD.

## Local verification, 2026-10-15

The complete Maven reactor passed on official Core 2.8.7 and 2.8.10 with Java 21:
1,924 tests per baseline, zero failures/errors and 14 skips. The verifier passed
its positive/negative fixtures and the OMOD built against Core 2.8.10. Core
2.8.9 remains part of the required CI matrix. External-server integration and
clinical acceptance have not been run for this release.

This result does **not** establish compatibility with the distribution's
`2.8.10-sihsalus.3` candidate: that Core dependency patch removes Commons Lang 2,
which REST 3.6.0 still uses. Compilation against the modified Core fails with
missing `org.apache.commons.lang` classes. Queue 3.1.0 and OCL 4.0.0 show the
same incompatibility. The Core patch must be reevaluated with security review;
this PR neither restores the dependency nor bypasses the image security gate.

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

## Candidate validation, 2026-10-02

Native tests ran in isolated DEV resources on Java 21, without application-data
mounts. All 772 tracked source/configuration files except this document matched
commit `b2b00e2bdd7e6d3bc3c6b89cb486096e2c876ebc`.

| Validation                                                                        | Result  | Evidence                                                              |
| --------------------------------------------------------------------------------- | ------- | --------------------------------------------------------------------- |
| Full reactor, Core 2.8.7                                                          | PASSED  | 1,924 tests, 14 skipped, no failures/errors; 407 seconds              |
| Full reactor, Core 2.8.9                                                          | PASSED  | 1,924 tests, 14 skipped, no failures/errors; 250 seconds              |
| `python3 tools/verify_omod.py --self-test`                                        | PASSED  | One positive and eight negative fixtures                              |
| `python3 tools/verify_omod.py omod/target/webservices.rest-3.5.1-sihsalus.2.omod` | PASSED  | Identity, version and compiled UTF-8/plain-text/nosniff/javax markers |
| External-server REST and synthetic form acceptance                                | NOT RUN | Native reactors do not establish deployed behavior                    |
| Release publication, attestation and distribution-pin update                      | NOT RUN | Candidate remains unpublished                                         |

For each Core version, the reactor command was:

```bash
mvn -B -ntp -Dstyle.color=never -Dformatter.skip=true -Dspotless.skip=true \
  -Dmaven.javadoc.skip=true -Dopenmrs.version=CORE_VERSION clean verify
```

The verified Core 2.8.9 OMOD has SHA-256
`06126c8005471be560120392b33613659b0f32e9b5889b733377fb5654bf192e`.
This identifies the tested local artifact; it is not a published or attested
release and must not be installed manually into a running distribution. Test
containers were removed; sources, logs and the candidate binary remain in the
isolated DEV test directory.

PR CI exposed a cache-test precondition on both Core versions before the
endpoint was called. Targeted DEV tests reproduced it. The cache-controller
fixture now starts a fresh rollback transaction after Core's committed base
fixture and cache resets, removes the specific fixture entries, and explicitly
reloads the target and a control through a cache-refreshing query. The old query
used an unrelated ID. The test verifies the returned target and that the control
remains cached after the selected target is evicted. Existing cache-presence,
HTTP-status and eviction assertions remain.

Both complete CI reactors must pass on the updated head before publication; the
earlier full DEV reactor results above remain evidence for their recorded source
revision. Final targeted DEV validation is recorded separately from those
reactors and from deployment acceptance.

Final targeted DEV validation passed on Java 21 for Core 2.8.7 and Core 2.8.9:
four cache-controller tests executed on each, without skips, failures or errors.
The Core 2.8.7 result was also repeated successfully. The tested cache-fixture
SHA-256 is `56c4cfd286687439ae7cd45c5f115c22fa3c739de1168cf5810132be9ee46246`.
These runs used the reactor command above with offline dependency resolution,
`-Dtest=ClearDbCacheController2_0Test`,
`-Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false`, and `clean package`.
Other test classes were deliberately excluded from this focused validation;
complete PR CI is still required on the final head.

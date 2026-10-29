# Repository guidance for agents

## Scope

This repository contains the public Simla and RetailCRM Android SDK. Keep changes focused on the
SDK, its tests, documentation, and build infrastructure.

The modules are:

- `sdk/core` — shared runtime and the `CrmSdk` entry point;
- `sdk/consultant` — online consultant API and UI;
- `sdk/tracker` — customer activity tracking;
- `sdk/recommendations` — product recommendations; depends on `tracker`;
- `sdk/push-firebase` — Firebase push integration;
- `sdk/bom` — compatible versions of the published modules.

Do not introduce reverse or cyclic module dependencies. Shared infrastructure belongs in `core`;
feature-specific code stays in its feature module.

## Documentation

Read `README.md` before making changes, then use the English Simla documentation as the primary
reference for agent work:

- `sdk/README.MD` — shared SDK API and behavior;
- `docs/simla/sdk.md` — Simla integration and repository guide;
- `docs/simla/consultant.md` — consultant UI and chat behavior;
- `docs/simla/tracker.md` — tracking;
- `docs/simla/recommendations.md` — recommendations;
- `docs/simla/push-firebase.md` — Firebase push notifications.

Contributor guidance, the changelog, the release procedure, vulnerability reporting, and licensing
are maintained as sections of `docs/simla/sdk.md` and its Russian counterpart,
`docs/retailcrm/sdk.md`.

`docs/simla` contains the English Simla documentation. `docs/retailcrm` contains the equivalent
Russian RetailCRM documentation. The two trees must describe the same SDK behavior while preserving
their language, brand name, Maven coordinates, and default server URL. When a document changes in
one tree, update its counterpart in the other tree in the same change.

Documentation must describe the current implementation, use fake example values, and avoid
internal discussion or temporary notes.

## Compatibility requirements

- Preserve backward compatibility unless a breaking change is explicitly requested.
- The intended public surface consists of `CrmSdk`, `CrmSdkConfig`, feature interfaces, public
  models, configuration types, and the Compose and Android View entry points.
- Keep network DTOs, controllers, storage implementations, dispatchers, and runtime internals out
  of the public API.
- Preserve both `simla` and `retailcrm` variants. They share source code but have different Maven
  coordinates, namespaces, metadata, and default server URLs.
- Preserve application-owned resources. In particular, do not close an `OkHttpClient` supplied by
  an integrating application.
- Customer switching must not mix sessions, messages, push registrations, or queued events.

Public API baselines are stored in each Android module's `api` directory. Check them with:

```shell
./gradlew apiCheckAll
```

For an intentional public API change, regenerate them with `./gradlew apiDumpAll` and review every
resulting `*.api` change. Do not update baselines only to silence a failed check.

## Implementation rules

- Use the committed Gradle Wrapper and versions from `gradle/libs.versions.toml`.
- Follow the existing Kotlin and Gradle Kotlin DSL style.
- Keep changes minimal and avoid unrelated formatting or renaming.
- Add or update tests for changed behavior, serialization, networking, lifecycle, persistence, UI,
  tracker queues, recommendations, and push handling as applicable.
- Preserve coroutine cancellation, thread safety, and existing scope ownership.
- Do not perform network or disk work on the main thread.
- Keep Compose and Android View entry points compatible when changing consultant UI behavior.
- Preserve public Android resources declared in `public.xml` and update all supported
  localizations when adding user-visible text.

## Security

Never commit real site, user, or session tokens; API keys; passwords; Firebase service accounts;
`google-services.json`; signing keystores; private keys; certificates; publication credentials; or
GPG keys.

The SDK relies on the Android system trust store. Do not disable TLS or hostname verification, add
a trust-all manager, or embed private credentials. Use placeholders such as `SITE_TOKEN`,
`site-token`, and `example.com` in tests and documentation.

## Verification

Run the focused tests for the affected module while developing. Before considering a change
complete, run:

```shell
./gradlew sdkTest apiCheckAll
./gradlew :sdk:core:assembleSimlaRelease :sdk:core:assembleRetailcrmRelease \
  :sdk:consultant:assembleSimlaRelease :sdk:consultant:assembleRetailcrmRelease \
  :sdk:tracker:assembleSimlaRelease :sdk:tracker:assembleRetailcrmRelease \
  :sdk:recommendations:assembleSimlaRelease :sdk:recommendations:assembleRetailcrmRelease \
  :sdk:push-firebase:assembleSimlaRelease :sdk:push-firebase:assembleRetailcrmRelease \
  :sdk:bom:build
```

Report checks that were not run or did not pass. Do not weaken or disable tests or CI checks to
make a change pass.

## Repository hygiene

Do not commit build output, IDE state, `local.properties`, or generated local artifacts. Preserve
unrelated user changes and review the final diff for secrets, local paths, generated files, and
unintended public API changes.

Do not change module versions, create release tags, publish artifacts, or publish GitHub Releases
without an explicit maintainer request. Follow the release process in `docs/simla/sdk.md` for
authorized releases.

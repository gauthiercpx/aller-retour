# Android app: plan and workflow

Scope: the Android side of Round Trip. Product goals and constraints live in
`.claude/CLAUDE.md`; this file is the build plan and the way we work on it.
Backend work is tracked on `feat/backend-mvp` and is out of scope here.

## Status

| Phase | State |
|---|---|
| 0. Foundation (Gradle, convention plugins, theme, CI) | Done, verified locally |
| 1. MVP widget | Built, gate green; not yet run on a device |
| 2 to 8 | Planned below |

## Stack (from the `claude-android-ninja` skill)

Kotlin, Compose, Glance, AGP 9.3 with built-in Kotlin, Gradle 9.8, compile/target SDK 37,
min SDK 24, Room 3 (phase 3), WorkManager, Hilt (added in phase 1), Navigation3 (added when
the app has more than one screen), Detekt 2 and Spotless for quality gates.
Versions are pinned in `gradle/libs.versions.toml`. Build logic is in `build-logic/`
(copied from the skill; four local fixes are listed under "Deviations from the skill").

## Modules

Built (phase 0 and 1):

```
:app             entry point, manifest, widget + worker, settings screen, DI graph
:core:model      pure Kotlin. Departure and friends: the backend wire contract
:core:domain     pure Kotlin. Time slot, stop plan, validation, upcoming-departure selection
:core:network    Retrofit client for the backend
:core:datastore  Preferences DataStore: settings and last response
:core:data       DeparturesRepository
:core:ui         theme (STAR red / IDFM blue)
```

Still to come, following the skill's dependency direction (feature -> core, never feature -> feature):

```
:core:database   Room 3: tap log, habit stats                        (phase 3)
:core:domain     grows: context engine, ranking, commute state machine (phase 2)
:feature:arrival Rive arrival overlay                                (phase 6)
:feature:settings places and stops UI, moved out of :app             (phase 2)
```

Widgets are not a feature module: Glance receivers stay in `:app` and read only from the
`:core:data` cache, so a widget render never waits on the network.

### Backend contract

`GET /departures?stops=<id>,<id>` returns `DeparturesResponse` (see `Departure.kt` on
`feat/backend-mvp`). `core/model` currently holds a **copy** of those classes. Duplicating
is deliberate for now; once the backend branch merges, extract one shared JVM module
(both Gradle builds consume it) rather than keeping two copies.
The app sends stop IDs only, never coordinates (privacy rule in CLAUDE.md).

## Phases

Each phase ends with the local gate (below) green and a short manual check on the device.

### 1. MVP widget
Built. How it works:
- `:core:domain` (pure Kotlin): `timeSlotOf` (before 12:00 is MORNING, else EVENING), `StopPlan`,
  stop-list and https-URL validation, `selectUpcoming` (next N departures, tolerates a 1 minute
  grace so a just-left train does not vanish between 15 minute refreshes).
- `:core:network`: Retrofit `GET /departures?stops=a,b`, failures mapped to `BackendException`.
- `:core:datastore`: one Preferences DataStore holding the settings and the last response, so
  the widget still has content offline. An unreadable cached payload counts as empty.
- `:core:data`: `DeparturesRepository.refresh()` fetches the stops planned for the current slot;
  a failed fetch keeps the old snapshot.
- `:app`: Hilt, `RefreshWorker` (15 minute periodic work, scheduled only while a widget exists;
  tap triggers a unique one-time refresh), Glance widget showing 2 departures with line badge,
  destination, absolute time, delay note and an "Updated HH:mm" footer, and a settings screen
  (backend URL, morning stops, evening stops).
- The city is implied by the stop ids (`idfm:`, `star-metro:`, `star-bus:`); phase 2 replaces the
  time-slot rule with real context.
- A failed fetch is not retried with backoff; the next period is the retry (battery).
- Tests: 52 unit tests across domain, network (MockWebServer), datastore, repository, formatting.

Still to do before calling the phase done: install on the phone, add the widget, and check it
against the real backend (needs the backend URL and your stop ids).

### 2. Context engine
- Places (home, work, uni, Rennes, Paris) and stops of interest in DataStore.
- Geofencing + Activity Recognition transitions feed the commute state machine
  `AT_HOME -> ... -> ARRIVED` in `:core:domain` (pure Kotlin, table-driven tests).
- Foreground service with live notification from `WALKING_TO_STOP` to `ARRIVED` only.
  Foreground service type and permissions per the skill's notifications and permissions
  references.
- Manual override ("I'm on the 8:42", "Not commuting") always wins.
- Permission flow staged: foreground location, then background location, activity
  recognition, notifications. Each requested at the moment its feature is switched on.

### 3. Habit learning
Room 3 table of taps/opens with context; plain counting to rank routes. No LLM.

### 4. Leave now
Walking time per stop + next departure gives "leave in X min". Nudge notification, one per trip.

### 5. Disruption-aware fallback
Needs an alerts field or endpoint on the backend (`Departure.alerts` exists but the feed is
not wired). Open a backend task before starting.

### 6. Vehicle illustrations and arrival animation
Original SVG templates per vehicle family with swappable colours (CLAUDE.md, Assets).
Rive overlay screen opened from the widget. The `confidence` field drives a "likely" badge.

### 7. AI features
Client side only: render the backend's one-line disruption summary and a natural-language
query screen. Never on the widget's critical path.

### 8. Rennes <-> Paris TGV mode
After the backend exposes the SNCF source.

## Cross-cutting rules

- **Battery:** nothing runs outside a commute except geofences, activity transitions and the
  15 minute worker. No continuous GPS; short bursts only to resolve ambiguity.
- **Design:** brand-seeded static colour scheme (no dynamic colour, no M3 purple), typography
  and line-coloured bars over stacked cards. Run the `avoid-ai-design` audit on each new screen.
- **Release:** personal sideloaded APK. No Play upload, no signing config in the repo; a
  release keystore, if ever needed, stays outside git.
- **Data sources:** check the `transit-data` skill before assuming a field exists.

## Workflow

### Branches
- One branch per phase or feature, `feat/android-<topic>`, cut from `main`.
- Another agent may be working on the app at the same time. Before starting, run
  `git worktree list` and `git branch --list 'feat/*'`; do not touch a branch someone else
  owns, and rebase on `main` rather than merging other feature branches into yours.
- Conventional Commits, one logical change per commit, explicit paths when staging.

### Local gate (run before saying a change is done)

```bash
./gradlew spotlessCheck detekt :app:lintDebug \
  :core:model:test :core:ui:testDebugUnitTest :app:testDebugUnitTest \
  :app:assembleDebug
```

`./gradlew spotlessApply` fixes formatting. After touching modules, DI, navigation, the
catalog or AGP/Kotlin versions, also run `./gradlew help` first.
Needs the Android SDK (`sdk.dir` in the git-ignored `local.properties`; platform 37 is installed).

### Per-change loop
1. **Before consuming a new endpoint or field:** `api-scout` agent / `transit-data` skill.
2. Implement in the smallest module that owns the change, with tests alongside.
3. Local gate green.
4. **Before committing:** `reviewer` agent (Kotlin, Compose/Glance quality, battery, secrets).
5. Anything about the container or cluster goes to `backend-ops`, not into this tree.

### CI (`.github/workflows/android.yml`)
Runs on pushes to `main` and on pull requests that touch Android paths: Spotless, Detekt,
lint, unit tests, debug APK (uploaded as an artifact). Debug only, no secrets, no signing.
Backend-only changes do not trigger it.

### Installing on the phone
Download the `round-trip-debug` artifact (or build locally) and `adb install -r` on the
personal device. Do not use `pm clear` or install on any device that is not yours.

## Open questions
1. **Reaching the backend from the phone:** Tailscale MagicDNS over plain HTTP, or HTTPS via
   `tailscale serve`? The answer decides whether we need a network security config. Default
   assumed: HTTPS, no cleartext exceptions.
2. **HTTP client:** the skill's stack is Retrofit + kotlinx.serialization; the backend uses
   Ktor client. Plan assumes Retrofit (skill default); say if you prefer Ktor for symmetry.
3. **Brand colours:** `StarRed` and `IdfmBlue` in `core/ui` are placeholders until checked
   against the operators' published charts.
4. **min SDK:** 24 comes from the skill template. Raise it to your phone's version if you
   want to drop desugaring.

## Deviations from the skill's templates

The templates did not build as shipped against the pinned toolchain; fixes applied in this repo:
1. `build-logic/convention/build.gradle.kts`: added the Spotless Gradle plugin dependency;
   removed the Firebase, Sentry and Play Vitals plugins (unused, and Firebase did not compile
   without its plugin dependencies).
2. `config/PrintApksTask.kt`: `artifacts.getAll(SingleArtifact.APK)` is no longer valid;
   now uses `artifacts.get`.
3. `config/ProjectExtensions.kt`: `Project.libs` made `internal`; public, it shadowed the
   generated `libs` accessor in module build scripts.
4. `config/detekt.yml`: the template uses detekt 1.x keys while the catalog pins detekt 2.0;
   kept only the Compose rules plus a `@Composable` naming exemption (also in `.editorconfig`
   for ktlint).

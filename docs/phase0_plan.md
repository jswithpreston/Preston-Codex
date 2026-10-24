# Phase 0 — Project Scaffold Plan (Revised)

## Goal
Bootstrap the Preston Codex Android project: install SDK packages, configure the Gradle
wrapper and version catalog, scaffold the package structure from spec §14.4, wire a minimal
`MainActivity`, add a CI workflow, and add a placeholder unit test. No feature code.

---

## 1 · Spec vs AGENTS.md Review (against full spec)

### ✅ Confirmed consistent

| Topic | AGENTS.md | Spec §14.3 |
|---|---|---|
| Layer names | domain / data / importer / android-integration / ui | domain / data / import / android-integration / presentation |
| domain + importer: no android.* | ✔ | ✔ (§14.3: "testable without launching Android screens") |
| All DB multi-record ops in transactions | ✔ | ✔ §5.7 |
| inject Clock, never call now() directly | ✔ | ✔ §3.5 ("local time zone … daylight-saving") |
| Never log private content | ✔ | ✔ §16 |
| Locked decision #1 (ID fingerprint order) | ✔ | ✔ §5.4 |
| Locked decision #3 (extraFields JSON) | ✔ | ✔ §5.1, §6 |
| Locked decision #4 (daily insight) | ✔ | ✔ §7.2 |
| Locked decision #5 (spaced repetition) | ✔ | ✔ §8.3 |
| Locked decision #6 (recurrence edits scope) | ✔ | ✔ §3.3 |
| Locked decision #7 (restore behavior) | ✔ | ✔ §13.2 |
| Locked decision #8 (trash 30 days) | ✔ | ✔ §13.4 |
| Locked decision #9 (flexible vs deadline reschedule) | ✔ | ✔ §3.4 |

### ⚠️ Minor naming differences (not contradictions)

| | AGENTS.md | Spec §14.3 |
|---|---|---|
| 3rd layer | "importer/" | "Import layer" |
| 5th layer | "ui" | "Presentation layer" |
| Package root | com.preston.codex | com.example.planner (§14.4 example only) |

§14.4 explicitly states its tree is a "starting organization, not a requirement." The package root
`com.preston.codex` (from AGENTS.md) is the binding decision.

### ⚠️ Decision #2: wording difference (documented, no behavioral impact)

| Source | Text |
|---|---|
| AGENTS.md | "normalized-token Jaccard similarity ≥ 0.85 and no exact/ID match" |
| Spec §5.4 / locked decision #2 in Preston-Codex.md | Adds "same or different source" qualifier before repeating the never-auto-merge rule |

Net behavior is identical: flag for review, never auto-merge regardless of source. No code impact.

### ⚠️ New finding: layer name mapping

Spec §14.4 has `notifications/` and `widget/` at the top-level package alongside `domain/`,
`data/`, `importer/`, `ui/`. AGENTS.md groups these under "android-integration layer." I will
map them as follows (approved by Q4 answer):

| Spec §14.4 package | AGENTS.md layer | Java package |
|---|---|---|
| `notifications/` | android-integration | `com.preston.codex.integration.notifications` |
| `widget/` | android-integration | `com.preston.codex.integration.widget` |

### ✅ Q2 answer recorded (daily insight with 1 eligible entry)
> If exactly one eligible entry exists, show it every day. Empty state shown only when zero
> eligible entries exist. This matches spec §7.2: "Handle a library containing only one
> eligible entry."

### ✅ Q3 answer recorded (nav label)
> Tab label is **"More"**; Settings lives inside it. Matches `design/more/`.

### ✅ Q4 answer recorded
> `com.preston.codex.integration` approved for the android-integration layer.

---

## 2 · Stitch Design Folder — Correction

**Previous claim (incorrect):** `design/stitch_preston_codex_app_design/` does not exist.

**Corrected:** `design/stitch_preston_codex_app_design/` **does exist**. It is a real directory
containing all the same screen sub-folders (import flows, today, library, review, tasks, more,
widgets, warm_editorial_scholar). It appears to be the overall app-design container referenced
in AGENTS.md alongside `warm_editorial_scholar/` as the design system. It is acknowledged —
no code action for Phase 0.

---

## 3 · Toolchain (Pinned — Sources Verified)

| Component | Version | Source |
|---|---|---|
| **AGP** | **8.13.0** | [developer.android.com release notes](https://developer.android.com/build/releases/past-releases/agp-8-13-0-release-notes) — September 2025 |
| **Gradle wrapper** | **8.13** | gradle.org — required by AGP 8.13.0 |
| **compileSdk / targetSdk** | **36** | Latest stable Android platform (Android 16) |
| **minSdk** | **26** | AGENTS.md (fixed) |
| **build-tools** | **35.0.0** | AGP 8.13.0 release notes list `35.0.0` as the default build-tools version; `36.1.0` is available but AGP selects 35 by default. Explicitly pinned to `35.0.0`. |
| **Room** | **2.8.5** | [developer.android.com](https://developer.android.com/jetpack/androidx/releases/room) — stable, released Sep 9 2026 |
| **WorkManager** | **2.12.0** | [developer.android.com](https://developer.android.com/jetpack/androidx/releases/work) — latest stable |
| **Material Components** | **1.14.0** | [material.io](https://github.com/material-components/material-components-android/releases) — final stable XML release |
| **desugar_jdk_libs** | **2.1.5** | [mvnrepository.com](https://mvnrepository.com/artifact/com.android.tools/desugar_jdk_libs/2.1.5) |
| **JUnit4** | **4.13.2** | [mvnrepository.com](https://mvnrepository.com/artifact/junit/junit/4.13.2) |
| **Mockito** | **5.14.2** | [mvnrepository.com](https://mvnrepository.com/artifact/org.mockito/mockito-core/5.14.2) — stable, released Oct 15 2024 |
| **Java source/target** | **17** | AGENTS.md (fixed); JDK 17.0.16 confirmed in environment |

> [!NOTE]
> **build-tools 35.0.0** is explicitly listed by AGP 8.13.0 as its default. AGP typically
> downloads build-tools automatically, but we install it via `sdkmanager` anyway for CI
> reproducibility. `36.1.0` is available in sdkmanager but not the AGP-recommended version.

> [!NOTE]
> **Material 1.14.0** is the final stable feature release for XML Views. Google has moved
> future development to Compose. This is the correct version for our XML + Java stack.

> [!NOTE]
> **Room 2.x** (Java/Android) — not Room 3.0 (KMP). Annotation processor: `annotationProcessor`
> (not `ksp`/`kapt`) because the project is Java-only.

---

## 4 · SDK Packages to Install

```bash
yes | sdkmanager --licenses > /dev/null
sdkmanager "platforms;android-36" "build-tools;35.0.0"
```

---

## 5 · Package Structure (from spec §14.4 + AGENTS.md layer mapping)

```
app/src/main/java/com/preston/codex/
├── domain/
│   ├── tasks/          ← task rules, recurrence, rescheduling policies
│   ├── knowledge/      ← identity matching, entry model
│   ├── recall/         ← review scheduling (spaced repetition)
│   └── scheduling/     ← daily insight selection
├── data/
│   ├── database/       ← Room database, type converters, migrations
│   ├── dao/            ← DAOs
│   ├── repository/     ← repositories
│   └── backup/         ← backup / restore / export
├── importer/           ← Markdown parsing, normalization, fingerprinting,
│                          cross-referencing, conflict classification
├── integration/        ← android-integration layer
│   ├── notifications/  ← WorkManager workers, AlarmManager, notification channels
│   └── widget/         ← AppWidgetProvider, RemoteViews
├── ui/                 ← Activities, Fragments, ViewModels (Phase 6+)
└── MainActivity.java
```

Each leaf package gets a `package-info.java` stub so the package is visible to the compiler
and future Javadoc. `domain/` and `importer/` must contain zero `android.*` imports.

---

## 6 · Files to Create (22 files total)

### Repo root / Gradle infrastructure

| Action | File | Purpose |
|---|---|---|
| NEW | `.gitignore` | Android-appropriate ignores |
| NEW | `gradle/wrapper/gradle-wrapper.properties` | Gradle 8.13-bin |
| NEW | `gradle/wrapper/gradle-wrapper.jar` | Wrapper binary |
| NEW | `gradlew` | Unix launcher (chmod +x) |
| NEW | `gradlew.bat` | Windows launcher |
| NEW | `settings.gradle` | rootProject.name, include ':app' |
| NEW | `build.gradle` | Root build file (AGP classpath) |
| NEW | `gradle/libs.versions.toml` | Version catalog |

### docs/

| Action | File | Purpose |
|---|---|---|
| NEW | `docs/DECISIONS.md` | Locked decisions log (required by Preston-Codex.md) |

### :app module

| Action | File | Purpose |
|---|---|---|
| NEW | `app/build.gradle` | Plugin, android{}, deps |
| NEW | `app/src/main/AndroidManifest.xml` | Minimal manifest |
| NEW | `app/src/main/res/values/strings.xml` | App name string |
| NEW | `app/src/main/res/values/themes.xml` | Placeholder Material3 theme |
| NEW | `app/src/main/res/layout/activity_main.xml` | Trivial layout |
| NEW | `app/src/main/java/com/preston/codex/MainActivity.java` | AppCompatActivity stub |

### Package stubs (package-info.java × 9)

| Package |
|---|
| `com.preston.codex.domain` |
| `com.preston.codex.domain.tasks` |
| `com.preston.codex.domain.knowledge` |
| `com.preston.codex.domain.recall` |
| `com.preston.codex.domain.scheduling` |
| `com.preston.codex.data` |
| `com.preston.codex.importer` |
| `com.preston.codex.integration` |
| `com.preston.codex.ui` |

*(sub-packages of data/ and integration/ are created as empty directories for now — package-info
added only for top-level packages to keep the file count manageable for Phase 0)*

### Test

| Action | File | Purpose |
|---|---|---|
| NEW | `app/src/test/java/com/preston/codex/domain/ClockTest.java` | JVM-only placeholder |

### CI

| Action | File | Purpose |
|---|---|---|
| NEW | `.github/workflows/android.yml` | Full CI pipeline |

---

## 7 · Key File Contents

### `gradle/libs.versions.toml`

```toml
[versions]
agp             = "8.13.0"
room            = "2.8.5"
workmanager     = "2.12.0"
material        = "1.14.0"
desugar         = "2.1.5"
junit           = "4.13.2"
mockito         = "5.14.2"

[libraries]
room-runtime        = { module = "androidx.room:room-runtime",          version.ref = "room" }
room-compiler       = { module = "androidx.room:room-compiler",         version.ref = "room" }
room-testing        = { module = "androidx.room:room-testing",          version.ref = "room" }
workmanager         = { module = "androidx.work:work-runtime",          version.ref = "workmanager" }
material            = { module = "com.google.android.material:material", version.ref = "material" }
desugar-jdk-libs    = { module = "com.android.tools:desugar_jdk_libs",  version.ref = "desugar" }
junit               = { module = "junit:junit",                         version.ref = "junit" }
mockito-core        = { module = "org.mockito:mockito-core",            version.ref = "mockito" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
```

### `app/build.gradle`

```groovy
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace 'com.preston.codex'
    compileSdk 36
    buildToolsVersion '35.0.0'

    defaultConfig {
        applicationId "com.preston.codex"
        minSdk 26
        targetSdk 36
        versionCode 1
        versionName "0.1.0"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        release { minifyEnabled false }
    }
}

dependencies {
    coreLibraryDesugaring libs.desugar.jdk.libs
    implementation libs.room.runtime
    annotationProcessor libs.room.compiler
    implementation libs.workmanager
    implementation libs.material
    testImplementation libs.junit
    testImplementation libs.mockito.core
}
```

### Placeholder unit test

```java
// ClockTest.java — zero android.* imports
package com.preston.codex.domain;

import org.junit.Test;
import java.time.Clock;
import java.time.Instant;
import static org.junit.Assert.assertNotNull;

public class ClockTest {
    @Test
    public void systemClock_returnsNonNullInstant() {
        Clock clock = Clock.systemUTC();
        Instant now = clock.instant();
        assertNotNull(now);
    }
}
```

Verifies: JVM test infra works; `java.time.Clock` (the injection interface used everywhere in
domain code) is functional on the test runner; no `android.*` imported in the `domain` package.

### `.github/workflows/android.yml`

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run unit tests
        run: ./gradlew test --stacktrace

      - name: Build debug APK
        run: ./gradlew assembleDebug --stacktrace

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

---

## 8 · Deviations from Spec

| # | Deviation | Reason | Impact |
|---|---|---|---|
| 1 | Package `com.preston.codex.integration` for "android-integration" layer | Hyphens illegal in Java package names | Approved (Q4) |
| 2 | Spec §14.4 uses `com.example.planner`; we use `com.preston.codex` | §14.4 explicitly labels its tree "a starting organization, not a requirement"; AGENTS.md fixes the package | None |
| 3 | `notifications/` and `widget/` sub-packages live under `integration/` rather than at root | Keeps AGENTS.md layer mapping intact | None |

---

## 9 · Amendments Applied (Approved Build)

The following amendments were applied before build:

1. **`gradle.properties`** added with `android.useAndroidX=true` and `org.gradle.jvmargs=-Xmx2048m`.
2. **`AndroidManifest.xml`** — `MainActivity` has `android:exported="true"`.
3. **`settings.gradle`** uses `pluginManagement` + `dependencyResolutionManagement` (google, mavenCentral, gradlePluginPortal). Root `build.gradle` uses `plugins { alias(...) apply false }` — no `buildscript` classpath block.
4. **`buildToolsVersion`** removed from `app/build.gradle`; AGP selects automatically. `platforms;android-36` still installed; build-tools installed after AGP requests its preferred version.
5. **Gradle wrapper** generated by downloading Gradle 8.13 binary to `/tmp`, running `gradle wrapper --gradle-version 8.13`, then deleting the download. `gradlew` is made executable.
6. **`package-info.java`** added to every package including all sub-packages (16 total).
7. **Shuffle / restore** settled by AGENTS.md locked decisions #4 and #7 respectively — no further question needed.
8. **No commit or push** — changes left uncommitted for review.

---

## 10 · Verification Plan

### Automated
```bash
./gradlew test            # ClockTest must pass (1 test, 0 failures)
./gradlew assembleDebug   # must produce app/build/outputs/apk/debug/app-debug.apk
```

### Manual checklist
- `app-debug.apk` present at expected path
- `gradle/libs.versions.toml` contains all pinned versions matching §3 table
- No `android.*` import in any file under `domain/` or `importer/`
- `.gitignore` covers: `*.iml`, `.gradle/`, `build/`, `local.properties`, `*.keystore`
- `docs/DECISIONS.md` records all 9 locked decisions from AGENTS.md

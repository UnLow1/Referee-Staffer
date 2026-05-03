# CLAUDE.md

Guidance for Claude Code when working in this repository. Captures the **current** state of the project — update as things change.

## Project

Spring Boot application for assigning football referees to matches. Personal / non-commercial test project, no real traffic, no active deployment target today.

Main class: `com.jamex.refereestaffer.RefereeStafferApplication`.

## Tech stack

- **Backend:** Spring Boot 4.0.6 (bumped from 3.5.6 on 2026-04-23, the same day 4.0.6 GA shipped), Java 25 (Corretto 25.0.2), Maven. Pulls in Spring Framework 7, Spring Security 7 (not used), Hibernate 7.1. Uses `jakarta.*` (JPA, validation) — migrated from `javax.*` in 2026-04. Jackson 2 still ships in SB 4 as deprecated — the single `@JsonIgnore` in `Grade.java` still imports from `com.fasterxml.jackson.annotation.*` and works; a future migration to Jackson 3 (`tools.jackson.*`) is possible but not urgent.
- **Tests:** Spock `2.4-groovy-4.0` (GA, released 2026-04) compiled with Groovy 4.0.31 via `gmavenplus-plugin` 4.3.1. Classifier/Groovy versions aligned — previously was a mismatch (Groovy 3 + `groovy-4.0` classifier). Spock 2.4 GA followed ~4 years of `2.4-M*` milestone development; if you see stale references to milestones or "Spock 2.3" anywhere, that's historical.
- **Frontend:** Angular 21 under `src/main/webapp/`, built by `frontend-maven-plugin` 2.0.0 as part of the Maven build (Node 24.15.0 Krypton LTS, npm 11.12.1 downloaded by the plugin). Migrated Angular 15 → 20 → 21 via step-by-step `ng update` chain in 2026-04. RxJS 7.8, TypeScript 5.9, zone.js 0.15. The Angular 20 → 21 `ng update` auto-migrated all templates to the `@if`/`@for` block control-flow syntax (`*ngIf`/`*ngFor` gone) and moved deprecated bootstrap options to providers in `main.ts`. Linting: ESLint 9 + `angular-eslint` + `typescript-eslint` (TSLint + codelyzer removed). No E2E tests — Protractor + `e2e/` removed (EOL 2023, no replacement wired up yet; Playwright/Cypress would be the natural choice if E2E coverage is ever added).
- **Other:** springdoc-openapi-starter-webmvc-ui 3.0.3 (Swagger UI; the 3.x line targets Spring Boot 4.x — 2.8.x is the SB 3 line), JaCoCo 0.8.14. OpenAPI spec served as 3.1.0 (was 3.0.1 under SB 3 + springdoc 2.8.x). **Lombok was removed 2026-04-24** — all DTOs, entities, services, and controllers now use hand-written constructors, getters/setters, `toString()`, and nested static `Builder` classes (DTOs and entities). SLF4J loggers are declared directly (`private static final Logger log = LoggerFactory.getLogger(X.class);`). Reason for removal: Lombok hooks into internal javac APIs and historically breaks on new JDK majors until patched; dropping it removes a compat risk for future Java versions. **Do not re-introduce Lombok** without a deliberate discussion.

## Build & run

- `mvn package` — builds backend **and** frontend together. `frontend-maven-plugin` installs Node/npm and runs `npm install` + `ng build` during `generate-resources`. **Do not build the frontend separately** unless you know why.
- `mvn test` — runs Spock specs. Surefire is configured with `<include>**/*Spec.java</include>`, which matches compiled Groovy specs.
- `mvn spring-boot:run` — runs locally with the default profile (in-memory H2).
- `cd src/main/webapp && npm start` — Angular dev server standalone, proxies API calls via `proxy.conf.json`.

## Tests

- Spock specs live under `src/test/groovy/com/jamex/refereestaffer/` (controllers, services, model converters).
- File naming convention: `*Spec.groovy` (not `*Test.java`).
- Parallel execution is enabled via `SpockConfig.groovy`.
- JaCoCo generates coverage reports to `target/site/jacoco/`. CI uploads `jacoco.xml` to Codecov (badge in README, PR comments). The full HTML report is also archived as a CI artifact. The Codecov upload uses `CODECOV_TOKEN` (GitHub repository secret) — public repo so tokenless mode also works, but the token avoids Codecov's tokenless rate-limiting/rejection. Rotate via Codecov repo settings → Reset Repository Upload Token, then update the GH Secret.

## Spring profiles and database

All three profiles currently use **H2** — there is no external database. The dialect is H2 in **MySQL compatibility mode** (`MODE=MySQL`).

| Profile | DB | `ddl-auto` | Notes |
|---|---|---|---|
| **default** (tests, bare `java -jar`) | H2 in-memory `jdbc:h2:mem:testdb` | `update` | `defer-datasource-initialization: true` so `data.sql` runs after Hibernate creates the schema |
| **`dev`** | H2 in-memory (inherited) | `create-drop` | Overlays default; schema dropped + recreated each run |
| **`prod`** | H2 **file-based** `jdbc:h2:file:./data/db/RefereeStaffer` | `update` (inherited) | `sql.init.mode: always` forces `data.sql` on every startup |

Seed data: `src/main/resources/data.sql` runs in **every** profile, but via different mechanisms:

- **default / dev**: `spring.sql.init.mode` is unset → defaults to `embedded` → H2 in-memory qualifies → seed runs.
- **prod**: explicit `spring.sql.init.mode: always` forces the seed (file-based H2 is still treated as embedded, but the explicit flag removes ambiguity).

**Gotcha for DB migration:** if the engine is ever switched to Postgres/MySQL, default and dev profiles **stop seeding** (no longer embedded); only prod keeps seeding via `always`. Change the mode explicitly if seed data is wanted everywhere.

**Dialect gotcha:** the app is tested against H2 in MySQL mode, not real MySQL and not Postgres. Known portability issues if the engine is swapped:

- `data.sql` uses `INSERT IGNORE` — MySQL-specific syntax. Postgres needs `INSERT ... ON CONFLICT DO NOTHING`.
- The `config.value` column name is a **reserved word in Postgres**. In H2 it is currently tolerated via `NON_KEYWORDS=VALUE` in the JDBC URL; Postgres will require quoting (`"value"`) or renaming the column.
- Native SQL in repositories (e.g. `RefereeRepository.findAllWithNoMatchInQueue`) uses unquoted table names like `match` — also a reserved word in some dialects.

## CI

GitHub Actions workflows under `.github/workflows/`, split by concern:

- `maven.yml` — **Backend only**. Builds Spring Boot with Maven, runs Spock specs, uploads `jacoco.xml` to Codecov, archives the full HTML JaCoCo report as an artifact. Triggers only on backend paths (`pom.xml`, `src/main/java/**`, `src/main/resources/**`, `src/test/**`). Builds with `-DskipFrontend=true` so the frontend-maven-plugin doesn't run.
- `frontend.yml` — **Frontend only**. Runs `npm ci`, `npm run build`, and `npm test` (Angular/Karma) on Node 24.15.0. Triggers only on `src/main/webapp/**`. Lint is **not** run — `npm run lint` currently surfaces ~134 pre-existing `@angular-eslint/prefer-inject` errors; re-enable once cleaned up.
- `codeql-analysis.yml` — GitHub CodeQL security scanning (Java + JavaScript).

**Split rationale:** the old arrangement built the frontend twice (once inside `mvn package` via `frontend-maven-plugin`, once in a separate `nodejs.yml`) and the frontend workflow was broken (`npm ci` → `npm run build` → `npm install` undid the deterministic install, `npm test` was commented out). Now backend and frontend each have one authoritative workflow with a `paths:` filter, so backend-only changes don't spin up a Node install.

**Skipping the frontend in Maven:** `pom.xml` defines a `skipFrontend` property (default `false`). Each `frontend-maven-plugin` execution respects `<skip>${skipFrontend}</skip>`, so `mvn -DskipFrontend=true package` skips install-node-and-npm, npm install, and the Angular build. Note: plugin-level `<skip>` and `-Dfrontend.skip` did not work reliably on 1.15.1 — the per-execution flag is the one we rely on. Behavior on 2.0.0 is assumed to be the same; the per-execution flag remains the authoritative toggle.

**Karma config gotcha:** `src/main/webapp/src/karma.conf.js` previously required `karma-coverage-istanbul-reporter`, which was removed in the Angular 15 → 20 migration, so `ng test` errored on startup. Now uses `karma-coverage` and defines a `ChromeHeadlessNoSandbox` custom launcher (base: `ChromeHeadless`, flags: `--no-sandbox --disable-gpu`) for CI. A duplicate `src/main/webapp/karma.conf.js` at the webapp root was dead (not referenced by `angular.json`) and was deleted.

**Action versions (as of 2026-04-24):**

- All workflows: `actions/checkout@v6`.
- Backend + CodeQL: `actions/setup-java@v5` on `distribution: temurin`, `java-version: 25`, `cache: maven`.
- `frontend.yml`: `actions/setup-node@v6`.
- `maven.yml`: `actions/upload-artifact@v7`, `codecov/codecov-action@v5`.
- `codeql-analysis.yml`: `github/codeql-action/{init,autobuild,analyze}@v3` (v1 was deprecated and rejected by GitHub during the 2026-04-23 modernization pass).

## Dependabot

`.github/dependabot.yml` — weekly Maven + npm updates, monthly GitHub Actions updates. Grouped so related packages land in one PR:

- Maven: `spring-boot`, `spring-ecosystem`, `test-stack` (Spock/Groovy/gmavenplus), `build-plugins` (maven plugins, JaCoCo, frontend-maven-plugin).
- npm: `angular`, `eslint`, `karma-jasmine`, `types`.
- GitHub Actions: ungrouped, monthly.

## CD

**None.** There is no active hosted environment. The `prod` profile exists but nothing runs it anywhere.

## Documentation drift

`README.md` has diverged from the actual implementation and environment — review before trusting it:

- **Badges**: a Snyk badge and a CodeClimate badge are still in the README. Activity/validity of those integrations is unverified — badges may be stale or broken. (Travis badge removed 2026-04-24 alongside `.travis.yml`.)
- **Algorithm formulas (LaTeX)**: the README describes the scoring algorithm with hard-coded "top 3" / "last 3" thresholds for edge matches, but the code uses the configurable `NUMBER_OF_EDGE_TEAMS` parameter. The formulas also do not fully reflect the current weights in `data.sql`. Treat the README math as **historical design notes**, not ground truth — `StafferService.countRefereePotentialLvl` and `MatchService.countHardnessLvl` are authoritative.

## Legacy / dead files

- `.travis.yml` — removed 2026-04-24. Travis CI free tier is effectively defunct; GitHub Actions (`maven.yml`, `frontend.yml`, `codeql-analysis.yml`) are now the single source of CI truth.
- `system.properties` — removed 2026-04-23. Was a Heroku Java runtime hint (`java.runtime.version=18`); Heroku free tier ended November 2022 and the project is not deployed there.
- Commit `0304975` (reverted by `e198a13`) attempted an AWS Aurora + Secrets Manager migration. Ignore these files if they appear in history/blame searches — they are not the direction of the project.

## Conventions

- Test files are named `*Spec.groovy`.
- The Swagger UI is available (springdoc-openapi-ui) when the app is running.
- Admin panel tip (from README): in the running app's browser DevTools, set `admin.hidden=false` to reveal the admin panel.

## Domain notes

- **"S C" referee sentinel** — in `RefereeService.getAvailableRefereesForQueue` matches assigned to a referee with `firstName="S"`, `lastName="C"` are filtered out of the auto-staffing pool. "S C" stands for **"Sędzia z Centrali"** (central-level referee assigned top-down by PZPN). The imported CSV (`data/import data file.csv`) marks such matches this way to signal the staffer must not overwrite the assignment. This is a string-based sentinel — not a great model. A future refactor should represent "do not reassign" as an explicit flag on `Match` (e.g. `centralAssignment: boolean`) or as a separate `Referee` type, so the filter isn't tied to a magic name.

## Repo layout (top level)

- `src/main/java/com/jamex/refereestaffer/` — Spring Boot sources
- `src/main/resources/` — `application*.yml`, `data.sql`, `example import.csv`
- `src/main/webapp/` — Angular 21 frontend (own `package.json`, own `eslint.config.js`)
- `src/test/groovy/com/jamex/refereestaffer/` — Spock specs
- `src/test/resources/` — `SpockConfig.groovy`, `test file.csv`
- `data/` — `import data file.csv` plus `screenshots/` referenced from `README.md`. Committed to the repo (not generated).
- `.github/workflows/` — CI workflows

# CLAUDE.md

Guidance for Claude Code when working in this repository. Captures the **current** state of the project — update as things change.

## Project

Spring Boot application for assigning football referees to matches. Personal / non-commercial test project, no real traffic, no active deployment target today.

Main class: `com.jamex.refereestaffer.RefereeStafferApplication`.

## Tech stack

- **Backend:** Spring Boot 4.0.6 (bumped from 3.5.6 on 2026-04-23, the same day 4.0.6 GA shipped), Java 25 (Corretto 25.0.2), Maven. Pulls in Spring Framework 7, Spring Security 7 (not used), Hibernate 7.1. Uses `jakarta.*` (JPA, validation) — migrated from `javax.*` in 2026-04. Jackson 2 still ships in SB 4 as deprecated — the single `@JsonIgnore` in `Grade.java` still imports from `com.fasterxml.jackson.annotation.*` and works; a future migration to Jackson 3 (`tools.jackson.*`) is possible but not urgent.
- **Tests:** Spock `2.4-M6-groovy-4.0` compiled with Groovy 4.0.28 via `gmavenplus-plugin` 3.0.2. Classifier/Groovy versions aligned — previously was a mismatch (Groovy 3 + `groovy-4.0` classifier). **Gotcha:** Spock has not released a stable 2.4 and has not patched 2.3 since September 2022; the `2.4-M*` milestone line has ~4 years of active development and is de facto the stable track. If you see stale "Spock 2.3" references anywhere, that's historical.
- **Frontend:** Angular 20 under `src/main/webapp/`, built by `frontend-maven-plugin` 1.15.1 as part of the Maven build (Node 22.22.2, npm 10.9.7 downloaded by the plugin). Migrated Angular 15 → 20 via step-by-step `ng update` chain in 2026-04. RxJS 7.8, TypeScript 5.9, zone.js 0.15. Linting: ESLint 9 + `angular-eslint` + `typescript-eslint` (TSLint + codelyzer removed). No E2E tests — Protractor + `e2e/` removed (EOL 2023, no replacement wired up yet; Playwright/Cypress would be the natural choice if E2E coverage is ever added).
- **Other:** Lombok 1.18.38 (annotation processor declared explicitly in `maven-compiler-plugin` — required since JDK 23 deprecated implicit annotation processing), springdoc-openapi-starter-webmvc-ui 3.0.3 (Swagger UI; the 3.x line targets Spring Boot 4.x — 2.8.x is the SB 3 line), JaCoCo 0.8.13. OpenAPI spec served as 3.1.0 (was 3.0.1 under SB 3 + springdoc 2.8.x).

## Build & run

- `mvn package` — builds backend **and** frontend together. `frontend-maven-plugin` installs Node/npm and runs `npm install` + `ng build` during `generate-resources`. **Do not build the frontend separately** unless you know why.
- `mvn test` — runs Spock specs. Surefire is configured with `<include>**/*Spec.java</include>`, which matches compiled Groovy specs.
- `mvn spring-boot:run` — runs locally with the default profile (in-memory H2).
- `cd src/main/webapp && npm start` — Angular dev server standalone, proxies API calls via `proxy.conf.json`.

## Tests

- Spock specs live under `src/test/groovy/com/jamex/refereestaffer/` (controllers, services, model converters).
- File naming convention: `*Spec.groovy` (not `*Test.java`).
- Parallel execution is enabled via `SpockConfig.groovy`.
- JaCoCo generates coverage reports to `target/site/jacoco/`. A coverage badge is auto-committed to `.github/badges/jacoco.svg` by the GitHub Actions workflow.

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

GitHub Actions workflows under `.github/workflows/`:

- `maven.yml` — Java/Maven build, JaCoCo badge generation and commit, coverage artifact upload.
- `nodejs.yml` — Node.js/Angular build (redundant with `mvn package`, but currently active).
- `codeql-analysis.yml` — GitHub CodeQL security scanning.

**Modernized 2026-04-23:**

- `maven.yml`: bumped to `actions/checkout@v4`, `actions/setup-java@v4` (`distribution: temurin`, `java-version: 25`, `cache: maven`), `actions/upload-artifact@v4`.
- `codeql-analysis.yml`: bumped CodeQL actions from v1 to v3 (`init@v3`, `autobuild@v3`, `analyze@v3`) — previously deprecated and rejected by GitHub. Also `actions/checkout@v4`, `actions/setup-java@v4` on Temurin 25.

**Still outstanding:**

- `nodejs.yml`: outdated actions (`actions/checkout@v2`, `actions/setup-node@v1`), pinned Node version is stale (frontend now needs Node 22), and step order is broken: runs `npm ci` → `npm run build` → `npm install` (the trailing `npm install` re-resolves from `package.json` and undoes the deterministic install). Workflow is also redundant with `mvn package` (which builds the frontend via `frontend-maven-plugin`). Strong candidate for deletion.

## CD

**None.** There is no active hosted environment. The `prod` profile exists but nothing runs it anywhere.

## Documentation drift

`README.md` has diverged from the actual implementation and environment — review before trusting it:

- **Badges**: includes a Travis CI badge (service is effectively dead for this project), a Snyk badge, and a CodeClimate badge. Activity/validity of Snyk and CodeClimate integrations is unverified — badges may be stale or broken.
- **Algorithm formulas (LaTeX)**: the README describes the scoring algorithm with hard-coded "top 3" / "last 3" thresholds for edge matches, but the code uses the configurable `NUMBER_OF_EDGE_TEAMS` parameter. The formulas also do not fully reflect the current weights in `data.sql`. Treat the README math as **historical design notes**, not ground truth — `StafferService.countRefereePotentialLvl` and `MatchService.countHardnessLvl` are authoritative.

## Legacy / dead files

- `.travis.yml` — Travis CI free tier is effectively defunct. Do not treat it as authoritative. (Kept in the repo for now since builds reportedly still run; will be removed once the new CI is verified.)
- `system.properties` — removed 2026-04-23. Was a Heroku Java runtime hint (`java.runtime.version=18`); Heroku free tier ended November 2022 and the project is not deployed there.
- Commit `0304975` (reverted by `e198a13`) attempted an AWS Aurora + Secrets Manager migration. Ignore these files if they appear in history/blame searches — they are not the direction of the project.

## Conventions

- Test files are named `*Spec.groovy`.
- The Swagger UI is available (springdoc-openapi-ui) when the app is running.
- Admin panel tip (from README): in the running app's browser DevTools, set `admin.hidden=false` to reveal the admin panel.

## Repo layout (top level)

- `src/main/java/com/jamex/refereestaffer/` — Spring Boot sources
- `src/main/resources/` — `application*.yml`, `data.sql`, `example import.csv`
- `src/main/webapp/` — Angular 20 frontend (own `package.json`, own `eslint.config.js`)
- `src/test/groovy/com/jamex/refereestaffer/` — Spock specs
- `src/test/resources/` — `SpockConfig.groovy`, `test file.csv`
- `data/` — `import data file.csv` plus `screenshots/` referenced from `README.md`. Committed to the repo (not generated).
- `.github/workflows/` — CI workflows
- `.github/badges/jacoco.svg` — auto-committed coverage badge

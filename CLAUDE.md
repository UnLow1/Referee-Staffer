# CLAUDE.md

Guidance for Claude Code when working in this repository. Captures the **current** state of the project — update as things change.

## Project

Spring Boot application for assigning football referees to matches. Personal / non-commercial test project, no real traffic, no active deployment target today.

Main class: `com.jamex.refereestaffer.RefereeStafferApplication`.

## Tech stack

- **Backend:** Spring Boot 2.7.5, Java 17, Maven
- **Tests:** Spock 2.3 (`spock-core:2.3-groovy-4.0`) compiled with Groovy 3.0.13 via `gmavenplus-plugin`. **Version mismatch:** the Spock artifact has the `groovy-4.0` classifier but the project pulls Groovy 3.0.13. Works due to backwards compatibility, but it is tech debt — align to either Groovy 3 + `spock-2.x-groovy-3.0` or Groovy 4 + `spock-2.x-groovy-4.0`.
- **Frontend:** Angular 15 under `src/main/webapp/`, built by `frontend-maven-plugin` as part of the Maven build (Node 16.13.0, npm 8.1.0 downloaded by the plugin)
- **Other:** Lombok 1.18.24, springdoc-openapi-ui 1.6.12 (Swagger UI), JaCoCo 0.8.7

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

**Known issues across workflows (not silently fix — flag first):**

- `maven.yml`: outdated actions — `actions/checkout@v2`, `actions/setup-java@v1`, `actions/upload-artifact@v2`. `java-version: 1.18` is wrong — the project is Java 17.
- `nodejs.yml`: outdated actions — `actions/checkout@v2`, `actions/setup-node@v1`. Step order is also broken: runs `npm ci` → `npm run build` → `npm install` (the trailing `npm install` re-resolves from `package.json` and undoes the deterministic install). Workflow is also redundant with `mvn package` (which builds the frontend via `frontend-maven-plugin`).
- `codeql-analysis.yml`: **CodeQL v1 actions are deprecated** (`github/codeql-action/init@v1`, `autobuild@v1`, `analyze@v1`) — GitHub will/does reject runs on these versions. This workflow likely fails or will fail soon. Also uses `actions/checkout@v2`. (Note: this workflow correctly sets `java-version: 17` with Zulu — inconsistent with `maven.yml`'s `1.18` typo.)

## CD

**None.** There is no active hosted environment. The `prod` profile exists but nothing runs it anywhere.

## Documentation drift

`README.md` has diverged from the actual implementation and environment — review before trusting it:

- **Badges**: includes a Travis CI badge (service is effectively dead for this project), a Snyk badge, and a CodeClimate badge. Activity/validity of Snyk and CodeClimate integrations is unverified — badges may be stale or broken.
- **Algorithm formulas (LaTeX)**: the README describes the scoring algorithm with hard-coded "top 3" / "last 3" thresholds for edge matches, but the code uses the configurable `NUMBER_OF_EDGE_TEAMS` parameter. The formulas also do not fully reflect the current weights in `data.sql`. Treat the README math as **historical design notes**, not ground truth — `StafferService.countRefereePotentialLvl` and `MatchService.countHardnessLvl` are authoritative.

## Legacy / dead files

- `.travis.yml` — Travis CI free tier is effectively defunct. Do not treat it as authoritative.
- `system.properties` — Heroku Java runtime hint. Heroku free tier ended November 2022.
- Commit `0304975` (reverted by `e198a13`) attempted an AWS Aurora + Secrets Manager migration. Ignore these files if they appear in history/blame searches — they are not the direction of the project.

## Conventions

- Test files are named `*Spec.groovy`.
- The Swagger UI is available (springdoc-openapi-ui) when the app is running.
- Admin panel tip (from README): in the running app's browser DevTools, set `admin.hidden=false` to reveal the admin panel.

## Repo layout (top level)

- `src/main/java/com/jamex/refereestaffer/` — Spring Boot sources
- `src/main/resources/` — `application*.yml`, `data.sql`, `example import.csv`
- `src/main/webapp/` — Angular 15 frontend (own `package.json`)
- `src/test/groovy/com/jamex/refereestaffer/` — Spock specs
- `src/test/resources/` — `SpockConfig.groovy`, `test file.csv`
- `data/` — `import data file.csv` plus `screenshots/` referenced from `README.md`. Committed to the repo (not generated).
- `.github/workflows/` — CI workflows
- `.github/badges/jacoco.svg` — auto-committed coverage badge

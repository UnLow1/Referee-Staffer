import { defineConfig, devices } from '@playwright/test';
import * as fs from 'node:fs';
import * as path from 'node:path';

// E2E configuration for the Referee-Staffer critical flow.
//
// The suite drives the real browser UI against a running Spring Boot instance that
// serves both the Angular SPA and the /api backend from a single origin (the fat jar
// produced by `mvn package`, with the frontend baked into src/main/resources/static).
// Playwright's webServer boots that jar; the default Spring profile uses an in-memory
// H2 database, so every run starts from an empty schema and the import step in the
// spec bootstraps all teams, referees, matches and grades from the CSV fixture.
// 8081, not the application's default 8080: a locally running dev instance would
// otherwise be picked up by reuseExistingServer, and the dev profile seeds its own
// teams and referees (the @Profile("dev") CommandLineRunner) — the import counts the
// spec asserts only hold against the empty default-profile schema.
const PORT = Number(process.env.E2E_PORT ?? 8081);
const baseURL = `http://127.0.0.1:${PORT}`;

// The Spring Boot fat jar, resolved here rather than handed to the shell as a glob:
// `java -jar ../../../target/*.jar` only works where the shell expands the pattern, so
// on Windows the literal glob reaches the JVM and the run dies with "Unable to access
// jarfile". Override with E2E_APP_JAR to point at an already-built artifact.
const targetDir = path.resolve(__dirname, '../../../target');

function resolveAppJar(): string {
  const override = process.env.E2E_APP_JAR;
  if (override) {
    return override;
  }
  // spring-boot-maven-plugin leaves the pre-repackage artifact as *.jar.original, so
  // an exact .jar suffix already selects the executable one.
  const jars = fs.existsSync(targetDir)
    ? fs.readdirSync(targetDir).filter((f) => f.endsWith('.jar'))
    : [];
  if (jars.length !== 1) {
    const found = jars.join(', ') || 'none';
    throw new Error(
      `Expected exactly one jar in ${targetDir}, found ${jars.length} (${found}). ` +
        'Run `mvn package` first, or set E2E_APP_JAR.',
    );
  }
  return path.join(targetDir, jars[0]);
}

const appJar = resolveAppJar();

export default defineConfig({
  testDir: './e2e',
  // The flow is stateful and shares one backend instance, so keep it strictly serial.
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL,
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      // 1440x900 keeps the shell above the 768px breakpoint, so the sidebar is a
      // static column (no off-canvas hamburger to deal with).
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
    },
  ],
  webServer: {
    // The port has to reach the JVM too, or Playwright would wait on PORT while the
    // app listens on its own default.
    command: `java -jar "${appJar}" --server.port=${PORT}`,
    url: baseURL,
    timeout: 120_000,
    // Locally, reuse a jar you already have running; in CI always boot a clean one.
    reuseExistingServer: !process.env.CI,
    // Surface the Spring Boot boot log: if the jar fails to start, the webServer
    // timeout alone is opaque, so pipe both streams into the Playwright output.
    stdout: 'pipe',
    stderr: 'pipe',
  },
});

import { defineConfig, devices } from '@playwright/test';

// E2E configuration for the Referee-Staffer critical flow.
//
// The suite drives the real browser UI against a running Spring Boot instance that
// serves both the Angular SPA and the /api backend from a single origin (the fat jar
// produced by `mvn package`, with the frontend baked into src/main/resources/static).
// Playwright's webServer boots that jar; the default Spring profile uses an in-memory
// H2 database, so every run starts from an empty schema and the import step in the
// spec bootstraps all teams, referees, matches and grades from the CSV fixture.
const PORT = Number(process.env.E2E_PORT ?? 8080);
const baseURL = `http://127.0.0.1:${PORT}`;

// The Spring Boot fat jar. Overridable so the CI job (or a manual run against an
// already-built artifact) can point at a specific path; the glob matches the single
// repackaged jar under target/ (the *.jar.original left by the plugin is skipped).
const appJar = process.env.E2E_APP_JAR ?? '../../../target/*.jar';

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
    command: `java -jar ${appJar}`,
    url: baseURL,
    timeout: 120_000,
    // Locally, reuse a jar you already have running; in CI always boot a clean one.
    reuseExistingServer: !process.env.CI,
    stdout: 'ignore',
    stderr: 'pipe',
  },
});

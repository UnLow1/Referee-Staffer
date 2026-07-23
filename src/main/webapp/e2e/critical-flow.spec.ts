import { test, expect } from '@playwright/test';
import * as path from 'node:path';

// End-to-end coverage of the application's critical flow, exercised through the real
// browser UI against a live backend (see playwright.config.ts):
//
//   import CSV  ->  generate staffing  ->  save the cast  ->  read standings
//
// The fixture (e2e/fixtures/import-data.csv) has three queues for four teams. It is
// imported with numberOfQueuesToImport = 2, so queues 1-2 are "played" (scores +
// referees -> they feed the standings) and queue 3 is upcoming with no referees, which
// is what the staffer assigns. The backend runs on a fresh in-memory H2 database per
// run, so the import bootstraps every entity from scratch and the counts are exact.

const CSV_PATH = path.join(__dirname, 'fixtures', 'import-data.csv');

// Derived from the fixture: 4 teams, 3 distinct referees, 6 matches, 4 graded rows
// (only the queue 1-2 rows carry a grade).
const EXPECTED = { referees: '3', teams: '4', matches: '6', grades: '4' };

// Queue 3 is the upcoming round the staffer casts (2 matches).
const STAFFED_QUEUE = 3;
const MATCHES_IN_STAFFED_QUEUE = 2;

test('critical flow: import CSV, generate and save staffing, read standings', async ({ page }) => {
  // The jar serves the SPA without a deep-link fallback (unknown paths 404), so the
  // suite loads the app root once and then moves between screens via the shell nav,
  // exactly as a user would.
  await test.step('import the CSV dataset', async () => {
    await page.goto('/');
    await page.getByRole('link', { name: 'Import data' }).click();
    await expect(page.getByRole('heading', { name: 'Import data' })).toBeVisible();

    await page.setInputFiles('#importerFile', CSV_PATH);
    await page.getByLabel('Number of queues to import').fill('2');

    await page.getByRole('button', { name: 'Import', exact: true }).click();

    const result = page.locator('.import-result');
    await expect(result).toBeVisible();
    await expect(result).toContainText('Import successful');

    const cellValue = (label: string) =>
      result.locator('.import-result__cell', { hasText: label }).locator('.import-result__value');
    await expect(cellValue('Teams')).toHaveText(EXPECTED.teams);
    await expect(cellValue('Referees')).toHaveText(EXPECTED.referees);
    await expect(cellValue('Matches')).toHaveText(EXPECTED.matches);
    await expect(cellValue('Grades')).toHaveText(EXPECTED.grades);
  });

  await test.step('generate the cast for the upcoming queue', async () => {
    await page.getByRole('link', { name: 'Staffer' }).click();
    await expect(page.getByRole('heading', { name: 'Staffer' })).toBeVisible();

    // Step from the default queue 1 up to the upcoming queue that still needs referees.
    for (let queue = 1; queue < STAFFED_QUEUE; queue++) {
      await page.getByRole('button', { name: 'Next queue' }).click();
    }
    await expect(page.getByRole('button', { name: `Queue ${STAFFED_QUEUE}` })).toBeVisible();

    await page.getByRole('button', { name: 'Generate cast' }).click();

    // The cast table appears with one row per match once staffing has persisted.
    await expect(page.locator('tr.cast-row')).toHaveCount(MATCHES_IN_STAFFED_QUEUE);
    // Every match must end up with an assigned referee (an avatar, never "unassigned"),
    // since the available pool covers the queue.
    await expect(page.locator('tr.cast-row .referee-cell app-ref-avatar'))
      .toHaveCount(MATCHES_IN_STAFFED_QUEUE);
    await expect(page.locator('tr.cast-row').getByText('unassigned')).toHaveCount(0);
  });

  await test.step('save the generated cast', async () => {
    await page.getByRole('button', { name: 'Save cast' }).click();
    await expect(page.locator('.saved-at')).toContainText('Saved');
  });

  await test.step('standings reflect the imported results', async () => {
    // Standings lives in the Admin nav group, hidden behind a toggle by default.
    await page.getByRole('button', { name: 'Show admin section' }).click();
    await page.getByRole('link', { name: 'Standings' }).click();
    await expect(page.getByRole('heading', { name: 'Standings' })).toBeVisible();

    // One row per imported team, and not the "No standings yet" empty state.
    await expect(page.locator('table.tbl tbody tr')).toHaveCount(Number(EXPECTED.teams));
    await expect(page.getByText('No standings yet')).toHaveCount(0);
    await expect(page.locator('table.tbl tbody')).toContainText('Alpha FC');
  });
});

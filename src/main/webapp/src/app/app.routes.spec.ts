import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {Router, provideRouter} from '@angular/router';
import {RouterTestingHarness} from '@angular/router/testing';
import {of} from 'rxjs';
import {routes} from './app.routes';
import {UiSettingsService} from './service/ui-settings.service';
import {MatchService} from './service/match.service';
import {RefereeService} from './service/referee.service';
import {TeamService} from './service/team.service';
import {ImporterService} from './service/importer.service';
import {createMock} from './testing/mock';

describe('app routes', () => {
  let router: Router;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    // Navigation renders the shell plus the target screen, so their services are
    // stubbed the same way the component specs do it.
    const matchService = createMock<MatchService>(['findAll']);
    matchService.findAll.mockReturnValue(of([]));
    const refereeService = createMock<RefereeService>(['findAll']);
    refereeService.findAll.mockReturnValue(of([]));
    const teamService = createMock<TeamService>(['getStandings']);
    teamService.getStandings.mockReturnValue(of({afterQueue: null, rows: []}));
    const importerService = createMock<ImporterService>(['postFile', 'downloadExampleFile']);

    const settings = {
      dark: signal(false),
      adminVisible: signal(false),
      explainerVisible: signal(false),
      toggleDark: vi.fn().mockName('toggleDark'),
      toggleAdmin: vi.fn().mockName('toggleAdmin'),
      toggleExplainer: vi.fn().mockName('toggleExplainer')
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        {provide: UiSettingsService, useValue: settings},
        {provide: MatchService, useValue: matchService},
        {provide: RefereeService, useValue: refereeService},
        {provide: TeamService, useValue: teamService},
        {provide: ImporterService, useValue: importerService}
      ]
    });

    harness = await RouterTestingHarness.create();
    router = TestBed.inject(Router);
  });

  it('redirects an unknown URL to the dashboard', async () => {
    await harness.navigateByUrl('/no-such-screen');

    expect(router.url).toBe('/dashboard');
  });

  it('redirects unknown nested URLs to the dashboard', async () => {
    await harness.navigateByUrl('/matches/1/definitely/not/a/route');

    expect(router.url).toBe('/dashboard');
  });

  it('sends deep paths under the legacy /importer to the dashboard, not /import', async () => {
    // The old importer screen had no child routes, so only the exact path is preserved.
    await harness.navigateByUrl('/importer/foo');

    expect(router.url).toBe('/dashboard');
  });

  it('redirects the legacy /importer path to /import', async () => {
    await harness.navigateByUrl('/importer');

    expect(router.url).toBe('/import');
  });

  it('still resolves known routes directly', async () => {
    await harness.navigateByUrl('/import');

    expect(router.url).toBe('/import');
  });
});

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

describe('app routes', () => {
  let router: Router;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    // Navigation renders the shell plus the target screen, so their services are
    // stubbed the same way the component specs do it.
    const matchService = jasmine.createSpyObj<MatchService>('MatchService', ['findAll']);
    matchService.findAll.and.returnValue(of([]));
    const refereeService = jasmine.createSpyObj<RefereeService>('RefereeService', ['findAll']);
    refereeService.findAll.and.returnValue(of([]));
    const teamService = jasmine.createSpyObj<TeamService>('TeamService', ['getStandings']);
    teamService.getStandings.and.returnValue(of([]));
    const importerService = jasmine.createSpyObj<ImporterService>('ImporterService', ['postFile', 'downloadExampleFile']);

    const settings = {
      dark: signal(false),
      adminVisible: signal(false),
      explainerVisible: signal(false),
      toggleDark: jasmine.createSpy('toggleDark'),
      toggleAdmin: jasmine.createSpy('toggleAdmin'),
      toggleExplainer: jasmine.createSpy('toggleExplainer')
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

  it('redirects the legacy /importer path to /import', async () => {
    await harness.navigateByUrl('/importer');

    expect(router.url).toBe('/import');
  });

  it('still resolves known routes directly', async () => {
    await harness.navigateByUrl('/import');

    expect(router.url).toBe('/import');
  });
});

import {ComponentFixture, TestBed, fakeAsync, tick} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router} from '@angular/router';
import {of} from 'rxjs';
import {RefereeProfileComponent} from './referee-profile.component';
import {RefereeService} from '../../service/referee.service';
import {MatchService} from '../../service/match.service';
import {TeamService} from '../../service/team.service';
import {GradeService} from '../../service/grade.service';
import {Match} from '../../model/match';
import {Referee} from '../../model/referee';
import {Team} from '../../model/team';
import {Grade} from '../../model/grade';

describe('RefereeProfileComponent', () => {
  let refereeService: jasmine.SpyObj<RefereeService>;
  let matchService: jasmine.SpyObj<MatchService>;
  let teamService: jasmine.SpyObj<TeamService>;
  let gradeService: jasmine.SpyObj<GradeService>;
  let navigateSpy: jasmine.Spy;

  function makeReferee(overrides: Partial<Referee> = {}): Referee {
    return {
      id: 7,
      firstName: 'Jan',
      lastName: 'Kowalski',
      email: 'jan@example.com',
      experience: 10,
      averageGrade: 8.25,
      lastQueue: 9,
      potential: 77.5,
      homeWins: 3,
      awayWins: 1,
      ...overrides
    };
  }

  const teams: Team[] = [
    {id: 1, name: 'Alfa', city: 'Krakow', points: 40},
    {id: 2, name: 'Beta', city: 'Krakow', points: 35},
    {id: 3, name: 'Gamma', city: 'Gdansk', points: 30}
  ];
  const grades: Grade[] = [{id: 500, value: 8.5}];

  function makeMatch(id: number, overrides: Partial<Match> = {}): Match {
    return {
      id,
      queue: 1,
      homeTeamId: 1,
      awayTeamId: 2,
      date: new Date('2026-03-01T12:00:00'),
      refereeId: 7,
      gradeId: undefined,
      homeScore: undefined,
      awayScore: undefined,
      ...overrides
    } as Match;
  }

  const allMatches: Match[] = [
    makeMatch(11, {queue: 5, gradeId: 500, homeScore: 2, awayScore: 1}),
    makeMatch(12, {queue: 7, homeTeamId: 2, awayTeamId: 3}),
    // Someone else's match — must be filtered out of the history.
    makeMatch(13, {queue: 8, refereeId: 999, gradeId: 600})
  ];

  beforeEach(() => {
    refereeService = jasmine.createSpyObj('RefereeService', ['findById']);
    matchService = jasmine.createSpyObj('MatchService', ['findAll']);
    teamService = jasmine.createSpyObj('TeamService', ['findByIds']);
    gradeService = jasmine.createSpyObj('GradeService', ['findByIds']);

    refereeService.findById.and.returnValue(of(makeReferee()));
    matchService.findAll.and.returnValue(of(allMatches));
    teamService.findByIds.and.returnValue(of(teams));
    gradeService.findByIds.and.returnValue(of(grades));
  });

  async function create(id?: number): Promise<ComponentFixture<RefereeProfileComponent>> {
    // Some tests re-create with different stubs — allow more than one create per test.
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [RefereeProfileComponent],
      providers: [
        provideRouter([]),
        {provide: RefereeService, useValue: refereeService},
        {provide: MatchService, useValue: matchService},
        {provide: TeamService, useValue: teamService},
        {provide: GradeService, useValue: gradeService},
        {
          provide: ActivatedRoute,
          useValue: {snapshot: {paramMap: convertToParamMap(id ? {id: String(id)} : {})}}
        }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(RefereeProfileComponent);
    navigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture.detectChanges();
    return fixture;
  }

  it('loads only this referee\'s matches and joins the ids they reference', async () => {
    const component = (await create(7)).componentInstance;

    expect(refereeService.findById).toHaveBeenCalledWith(7);
    expect(component.matches().map(m => m.id)).toEqual([11, 12]);
    expect(teamService.findByIds).toHaveBeenCalledWith([1, 2, 3]);
    expect(gradeService.findByIds).toHaveBeenCalledWith([500]);
    expect(component.getTeam(1)?.name).toBe('Alfa');
    expect(component.getGrade(500)?.value).toBe(8.5);
    expect(component.getGrade(undefined)).toBeUndefined();
  });

  it('redirects to the referee list when the id param is missing', async () => {
    await create();

    expect(navigateSpy).toHaveBeenCalledWith(['/referees']);
    expect(refereeService.findById).not.toHaveBeenCalled();
  });

  it('skips the team and grade lookups for a referee with no matches', fakeAsync(() => {
    matchService.findAll.and.returnValue(of([makeMatch(13, {refereeId: 999})]));

    let component!: RefereeProfileComponent;
    create(7).then(fixture => component = fixture.componentInstance);
    // The empty-id branches resolve through Promise.resolve — flush the microtasks.
    tick();

    expect(teamService.findByIds).not.toHaveBeenCalled();
    expect(gradeService.findByIds).not.toHaveBeenCalled();
    expect(component.matches()).toEqual([]);
    expect(component.topCities()).toEqual([]);
  }));

  it('derives the hero labels from the referee', async () => {
    const component = (await create(7)).componentInstance;

    expect(component.initials()).toBe('JK');
    expect(component.fullName()).toBe('Jan Kowalski');
    expect(component.idLabel()).toBe('#007');
  });

  it('sorts the history newest queue first', async () => {
    const component = (await create(7)).componentInstance;

    expect(component.history().map(m => m.id)).toEqual([12, 11]);
    expect(component.gradedMatchCount()).toBe(1);
  });

  it('prefers server-computed stats and falls back to local derivations', async () => {
    const enriched = (await create(7)).componentInstance;
    expect(enriched.lastQueue()).toBe(9);
    expect(enriched.avgGrade()).toBe(8.25);
    expect(enriched.potential()).toBe(77.5);

    refereeService.findById.and.returnValue(of(makeReferee(
      {averageGrade: undefined, lastQueue: undefined, potential: undefined})));
    const local = (await create(7)).componentInstance;
    // Highest queue in the loaded history, and the only loaded grade.
    expect(local.lastQueue()).toBe(7);
    expect(local.avgGrade()).toBe(8.5);
    expect(local.potential()).toBeNull();
  });

  it('reports no average grade when neither the server nor the history has one', async () => {
    refereeService.findById.and.returnValue(of(makeReferee({averageGrade: undefined})));
    // The referenced grade does not resolve — the local calc has nothing to average.
    matchService.findAll.and.returnValue(of([makeMatch(12, {queue: 7, gradeId: 600})]));
    gradeService.findByIds.and.returnValue(of([]));

    const component = (await create(7)).componentInstance;

    expect(component.avgGrade()).toBeNull();
    expect(component.formatGrade(null)).toBe('—');
    expect(component.formatGrade(8.5)).toBe('8.50');
  });

  it('exposes the win balance only when a win was recorded', async () => {
    const component = (await create(7)).componentInstance;
    expect(component.homeWins()).toBe(3);
    expect(component.awayWins()).toBe(1);
    expect(component.hasWinData()).toBeTrue();

    refereeService.findById.and.returnValue(of(makeReferee({homeWins: undefined, awayWins: undefined})));
    const fresh = (await create(7)).componentInstance;
    expect(fresh.homeWins()).toBe(0);
    expect(fresh.hasWinData()).toBeFalse();
  });

  it('counts officiated cities per match side and scales the bars', async () => {
    const component = (await create(7)).componentInstance;

    // Match 11: Krakow + Krakow, match 12: Krakow + Gdansk.
    expect(component.topCities()).toEqual([
      {city: 'Krakow', count: 3},
      {city: 'Gdansk', count: 1}
    ]);
    expect(component.topCitiesMax()).toBe(3);
    expect(component.cityBarWidth(3)).toBe('100%');
  });

  it('renders the score line only when both halves are present', async () => {
    const component = (await create(7)).componentInstance;

    expect(component.scoreLine(allMatches[0])).toBe('2 – 1');
    expect(component.scoreLine(allMatches[1])).toBeNull();
    expect(component.gradeAsMeter(grades[0])).toBe(85);
    expect(component.gradeAsMeter(undefined)).toBe(0);
  });

  it('routes the edit action to the referee form deep-link', async () => {
    const component = (await create(7)).componentInstance;

    component.editReferee();

    expect(navigateSpy).toHaveBeenCalledWith(['/addReferee', 7]);
  });
});

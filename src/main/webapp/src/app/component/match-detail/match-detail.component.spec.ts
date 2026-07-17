import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router} from '@angular/router';
import {of} from 'rxjs';
import {MatchDetailComponent} from './match-detail.component';
import {MatchService} from '../../service/match.service';
import {TeamService} from '../../service/team.service';
import {RefereeService} from '../../service/referee.service';
import {Match} from '../../model/match';
import {Team} from '../../model/team';
import {Referee} from '../../model/referee';
import {DifficultyBreakdown} from '../../model/difficultyBreakdown';

describe('MatchDetailComponent', () => {
  let matchService: jasmine.SpyObj<MatchService>;
  let teamService: jasmine.SpyObj<TeamService>;
  let refereeService: jasmine.SpyObj<RefereeService>;
  let navigateSpy: jasmine.Spy;

  // Standings order defines place: index 0 = 1st. Teams 1 and 2 share a city so the
  // local same-city fallback has something to detect.
  const standings: Team[] = [
    {id: 1, name: 'Alfa', city: 'Krakow', points: 40},
    {id: 2, name: 'Beta', city: 'Krakow', points: 35},
    {id: 3, name: 'Gamma', city: 'Gdansk', points: 30},
    {id: 4, name: 'Delta', city: 'Poznan', points: 25},
    {id: 5, name: 'Epsilon', city: 'Lodz', points: 20},
    {id: 6, name: 'Zeta', city: 'Wroclaw', points: 15},
    {id: 7, name: 'Eta', city: 'Radom', points: 10},
    {id: 8, name: 'Theta', city: 'Opole', points: 5}
  ];

  function makeReferee(id: number, overrides: Partial<Referee> = {}): Referee {
    return {
      id,
      firstName: `First${id}`,
      lastName: `Last${id}`,
      email: `ref${id}@example.com`,
      experience: 1,
      ...overrides
    };
  }

  const referees: Referee[] = [
    makeReferee(100, {potential: 80}),
    // No potential — ranking falls back to experience.
    makeReferee(101, {potential: undefined, experience: 90}),
    makeReferee(102, {potential: 70})
  ];

  function makeMatch(overrides: Partial<Match> = {}): Match {
    return {
      id: 11,
      queue: 3,
      homeTeamId: 1,
      awayTeamId: 2,
      date: new Date('2026-03-01T12:00:00'),
      refereeId: 100,
      gradeId: undefined,
      homeScore: undefined,
      awayScore: undefined,
      ...overrides
    } as Match;
  }

  const breakdown: DifficultyBreakdown = {
    matchId: 11,
    total: 123,
    parts: {base: 100, sameCity: 0, top: 0, bottom: 23},
    // Deliberately contradicts the local derivation (same city, both top) to prove
    // the backend flags win once loaded.
    flags: {sameCity: false, isTop: false, isBot: true, pointsDiff: 7}
  };

  beforeEach(() => {
    matchService = jasmine.createSpyObj('MatchService', ['findById', 'getDifficultyBreakdown', 'update']);
    teamService = jasmine.createSpyObj('TeamService', ['getStandings']);
    refereeService = jasmine.createSpyObj('RefereeService', ['findAll']);

    matchService.findById.and.returnValue(of(makeMatch()));
    matchService.getDifficultyBreakdown.and.returnValue(of(breakdown));
    teamService.getStandings.and.returnValue(of(standings));
    refereeService.findAll.and.returnValue(of(referees));
  });

  async function create(id?: number): Promise<ComponentFixture<MatchDetailComponent>> {
    // Some tests re-create with different stubs — allow more than one create per test.
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [MatchDetailComponent],
      providers: [
        provideRouter([]),
        {provide: MatchService, useValue: matchService},
        {provide: TeamService, useValue: teamService},
        {provide: RefereeService, useValue: refereeService},
        {
          provide: ActivatedRoute,
          useValue: {snapshot: {paramMap: convertToParamMap(id ? {id: String(id)} : {})}}
        }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(MatchDetailComponent);
    navigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture.detectChanges();
    return fixture;
  }

  it('loads the match and joins teams, standings and the assigned referee', async () => {
    const component = (await create(11)).componentInstance;

    expect(matchService.findById).toHaveBeenCalledWith(11);
    expect(matchService.getDifficultyBreakdown).toHaveBeenCalledWith(11);
    expect(component.home()?.name).toBe('Alfa');
    expect(component.away()?.name).toBe('Beta');
    expect(component.assignedReferee()?.id).toBe(100);
    expect(component.homePlace()).toBe(1);
    expect(component.awayPlace()).toBe(2);
    expect(component.pointsDiff()).toBe(5);
  });

  it('redirects to the match list when the id param is missing', async () => {
    await create();

    expect(navigateSpy).toHaveBeenCalledWith(['/matches']);
    expect(matchService.findById).not.toHaveBeenCalled();
  });

  it('prefers the backend breakdown flags over the local derivation', async () => {
    const component = (await create(11)).componentInstance;

    expect(component.sameCity()).toBeFalse();
    expect(component.isTopMatch()).toBeFalse();
    expect(component.isRelegationMatch()).toBeTrue();
  });

  it('falls back to locally derived flags while the breakdown is absent', async () => {
    matchService.getDifficultyBreakdown.and.returnValue(of(null as unknown as DifficultyBreakdown));

    const component = (await create(11)).componentInstance;

    // Places 1 and 2, shared city — the local derivation sees a top-of-table derby.
    expect(component.sameCity()).toBeTrue();
    expect(component.isTopMatch()).toBeTrue();
    expect(component.isRelegationMatch()).toBeFalse();
  });

  it('derives a relegation match locally when both teams sit in the bottom three', async () => {
    matchService.getDifficultyBreakdown.and.returnValue(of(null as unknown as DifficultyBreakdown));
    matchService.findById.and.returnValue(of(makeMatch({homeTeamId: 7, awayTeamId: 8})));

    const component = (await create(11)).componentInstance;

    expect(component.isRelegationMatch()).toBeTrue();
    expect(component.isTopMatch()).toBeFalse();
    expect(component.sameCity()).toBeFalse();
  });

  it('ranks candidates by potential and marks the assigned one', async () => {
    const component = (await create(11)).componentInstance;

    expect(component.candidates().map(c => c.referee.id)).toEqual([101, 100, 102]);
    expect(component.candidates().map(c => c.isAssigned)).toEqual([false, true, false]);
  });

  it('assigns a referee through the update endpoint', async () => {
    const saved = makeMatch({refereeId: 102});
    matchService.update.and.returnValue(of(saved));
    const component = (await create(11)).componentInstance;

    component.assign(referees[2]);

    expect(matchService.update).toHaveBeenCalledWith(jasmine.objectContaining({id: 11, refereeId: 102}));
    expect(component.match()).toEqual(saved);
    expect(component.assignedReferee()?.id).toBe(102);
  });

  it('builds the compare rows from the joined teams', async () => {
    const component = (await create(11)).componentInstance;

    expect(component.compareRows()).toEqual([
      {label: 'Position', home: '#1', away: '#2', bar: false},
      {label: 'Points', home: 40, away: 35, bar: true}
    ]);
  });

  it('renders the score line only when both halves are present', async () => {
    const component = (await create(11)).componentInstance;
    expect(component.scoreLine()).toBeNull();

    matchService.findById.and.returnValue(of(makeMatch({homeScore: 2, awayScore: 1})));
    expect((await create(11)).componentInstance.scoreLine()).toBe('2 – 1');
  });

  it('normalises mirrored bars against the larger side', async () => {
    const component = (await create(11)).componentInstance;

    expect(component.barWidth(40, 35)).toBe('100%');
    expect(component.barWidth(35, 40)).toBe('87.5%');
    expect(component.barWidth(0, 0)).toBe('0%');
  });
});

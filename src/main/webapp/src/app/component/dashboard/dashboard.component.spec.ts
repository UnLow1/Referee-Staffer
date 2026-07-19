import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of} from 'rxjs';
import {DashboardComponent} from './dashboard.component';
import {MatchService} from '../../service/match.service';
import {RefereeService} from '../../service/referee.service';
import {TeamService} from '../../service/team.service';
import {Match} from '../../model/match';
import {Referee} from '../../model/referee';
import {Team} from '../../model/team';

describe('DashboardComponent', () => {
  let matchService: jasmine.SpyObj<MatchService>;
  let refereeService: jasmine.SpyObj<RefereeService>;
  let teamService: jasmine.SpyObj<TeamService>;

  // /api/teams/standings returns table order; 8 teams so both edge zones exist.
  const standings: Team[] = Array.from({length: 8}, (_, i) => ({
    id: i + 1, name: `Team ${i + 1}`, city: `City ${i + 1}`, points: 40 - i * 5
  }));

  function makeMatch(id: number, overrides: Partial<Match> = {}): Match {
    return {
      id,
      queue: 1,
      homeTeamId: 1,
      awayTeamId: 2,
      date: new Date('2026-03-01T12:00:00'),
      refereeId: undefined,
      gradeId: undefined,
      homeScore: undefined,
      awayScore: undefined,
      ...overrides
    } as Match;
  }

  const matches: Match[] = [
    // Queue 1 fully played — must not count as upcoming.
    makeMatch(11, {queue: 1, homeScore: 2, awayScore: 0, hardnessLvl: 50}),
    // Queue 2 has unplayed fixtures → it is the upcoming queue.
    makeMatch(12, {queue: 2, hardnessLvl: 80.2}),
    makeMatch(13, {queue: 2, homeTeamId: 3, awayTeamId: 4, hardnessLvl: 120.4}),
    // A later queue must not steal the "upcoming" slot.
    makeMatch(14, {queue: 3, homeTeamId: 5, awayTeamId: 6, hardnessLvl: 10})
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
    makeReferee(1, {potential: 90, averageGrade: 8}),
    // No potential — sorting and formatting fall back to experience.
    makeReferee(2, {potential: undefined, experience: 95, averageGrade: 7}),
    makeReferee(3, {potential: 70, averageGrade: undefined})
  ];

  beforeEach(async () => {
    matchService = jasmine.createSpyObj('MatchService', ['findAll']);
    refereeService = jasmine.createSpyObj('RefereeService', ['findAll']);
    teamService = jasmine.createSpyObj('TeamService', ['getStandings']);
    matchService.findAll.and.returnValue(of(matches));
    refereeService.findAll.and.returnValue(of(referees));
    teamService.getStandings.and.returnValue(of(standings));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        {provide: MatchService, useValue: matchService},
        {provide: RefereeService, useValue: refereeService},
        {provide: TeamService, useValue: teamService}
      ]
    }).compileComponents();
  });

  function create(): ComponentFixture<DashboardComponent> {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('picks the earliest queue with an unplayed match and sorts it by difficulty', () => {
    const component = create().componentInstance;

    expect(component.upcomingQueue()).toBe(2);
    expect(component.upcomingMatches().map(m => m.id)).toEqual([13, 12]);
    expect(component.upcomingDifficulty()).toBe(201);
  });

  it('reports no upcoming queue once every match is played', () => {
    matchService.findAll.and.returnValue(of([matches[0]]));

    const component = create().componentInstance;

    expect(component.upcomingQueue()).toBeNull();
    expect(component.upcomingMatches()).toEqual([]);
    expect(component.upcomingDifficulty()).toBe(0);
  });

  it('ranks referees by potential with an experience fallback', () => {
    const component = create().componentInstance;

    // Referee 2 has no potential but 95 experience — it outranks potential 90.
    expect(component.topReferees().map(r => r.id)).toEqual([2, 1, 3]);
    expect(component.topRefereesMaxPotential()).toBe(95);
    expect(component.potentialPct(referees[0])).toBe(95);
    expect(component.formatPotential(referees[0])).toBe('90');
    expect(component.formatPotential(referees[1])).toBe('95');
  });

  it('caps the top-referees list at seven', () => {
    refereeService.findAll.and.returnValue(of(
      Array.from({length: 10}, (_, i) => makeReferee(i + 1, {potential: 100 - i}))
    ));

    expect(create().componentInstance.topReferees().length).toBe(7);
  });

  it('averages observer grades across referees that have one', () => {
    const component = create().componentInstance;

    // Referee 3 has no grade and must not drag the average down.
    expect(component.avgObserverGrade()).toBe(7.5);
    expect(component.formatAvg(component.avgObserverGrade())).toBe('7.5');

    refereeService.findAll.and.returnValue(of([makeReferee(3, {averageGrade: undefined})]));
    const withoutGrades = create().componentInstance;
    expect(withoutGrades.avgObserverGrade()).toBeNull();
    expect(withoutGrades.formatAvg(null)).toBe('—');
  });

  it('marks the standings zones for the top and bottom three', () => {
    const component = create().componentInstance;

    expect([0, 1, 2].map(i => component.zoneFor(i))).toEqual(['top', 'top', 'top']);
    expect(component.zoneFor(3)).toBeNull();
    expect([5, 6, 7].map(i => component.zoneFor(i))).toEqual(['relegation', 'relegation', 'relegation']);
  });

  it('suppresses the relegation zone in a league too small for both zones', () => {
    teamService.getStandings.and.returnValue(of(standings.slice(0, 5)));

    const component = create().componentInstance;

    expect(component.zoneFor(2)).toBe('top');
    expect(component.zoneFor(4)).toBeNull();
  });

  it('scales points bars against the leader and flags three-digit difficulty', () => {
    const component = create().componentInstance;

    expect(component.pointsBarPct(standings[0])).toBe(100);
    expect(component.pointsBarPct(standings[4])).toBe(50);
    expect(component.difficultyKind(99)).toBe('default');
    expect(component.difficultyKind(100)).toBe('warn');
    expect(component.getTeam(3)?.name).toBe('Team 3');
    expect(component.getTeam(undefined)).toBeUndefined();
  });
});

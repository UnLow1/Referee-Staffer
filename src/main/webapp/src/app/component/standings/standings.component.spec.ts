import {ComponentFixture, TestBed} from '@angular/core/testing';
import {signal, WritableSignal} from '@angular/core';
import {of} from 'rxjs';
import {StandingsComponent} from './standings.component';
import {TeamService} from '../../service/team.service';
import {MatchService} from '../../service/match.service';
import {ConfigurationService} from '../../service/configuration.service';
import {Team} from '../../model/team';
import {Match} from '../../model/match';

describe('StandingsComponent', () => {
  let teamService: jasmine.SpyObj<TeamService>;
  let matchService: jasmine.SpyObj<MatchService>;
  let edgeTeams: WritableSignal<number>;

  // /api/teams/standings returns table order; 8 teams so both edge zones exist.
  const standings: Team[] = Array.from({length: 8}, (_, i) => ({
    id: i + 1, name: `Team ${i + 1}`, city: `City ${i + 1}`, points: 40 - i * 5
  }));

  const matches = [
    // Team 1 beats Team 2 away: 1 has W 3:1, 2 has L.
    {id: 11, queue: 1, homeTeamId: 2, awayTeamId: 1, homeScore: 1, awayScore: 3},
    // Draw between 1 and 3.
    {id: 12, queue: 2, homeTeamId: 1, awayTeamId: 3, homeScore: 2, awayScore: 2},
    // Unplayed — must not count anywhere.
    {id: 13, queue: 3, homeTeamId: 1, awayTeamId: 4}
  ] as Match[];

  beforeEach(async () => {
    teamService = jasmine.createSpyObj('TeamService', ['getStandings']);
    matchService = jasmine.createSpyObj('MatchService', ['findAll']);
    teamService.getStandings.and.returnValue(of(standings));
    matchService.findAll.and.returnValue(of(matches));
    edgeTeams = signal(3);

    await TestBed.configureTestingModule({
      imports: [StandingsComponent],
      providers: [
        {provide: TeamService, useValue: teamService},
        {provide: MatchService, useValue: matchService},
        {provide: ConfigurationService, useValue: {edgeTeams: edgeTeams.asReadonly(), ensureEdgeTeamsLoaded: jasmine.createSpy('ensureEdgeTeamsLoaded')}}
      ]
    }).compileComponents();
  });

  function create(): ComponentFixture<StandingsComponent> {
    const fixture = TestBed.createComponent(StandingsComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('derives W/D/L and goals from played matches only', () => {
    const rows = create().componentInstance.rows();
    const team1 = rows.find(r => r.team.id === 1)!;
    const team2 = rows.find(r => r.team.id === 2)!;
    const team4 = rows.find(r => r.team.id === 4)!;

    expect(team1).toEqual(jasmine.objectContaining(
      {played: 2, wins: 1, draws: 1, losses: 0, goalsFor: 5, goalsAgainst: 3}));
    expect(team2).toEqual(jasmine.objectContaining(
      {played: 1, wins: 0, draws: 0, losses: 1, goalsFor: 1, goalsAgainst: 3}));
    // Only fixture 13 references team 4 and it has no result.
    expect(team4.played).toBe(0);
  });

  it('marks the top three accent and the bottom three danger', () => {
    const rows = create().componentInstance.rows();

    expect(rows.filter(r => r.zone === 'accent').map(r => r.pos)).toEqual([1, 2, 3]);
    expect(rows.filter(r => r.zone === 'danger').map(r => r.pos)).toEqual([6, 7, 8]);
    expect(rows.filter(r => r.zone === null).map(r => r.pos)).toEqual([4, 5]);
  });

  it('suppresses the danger zone in a league too small for both zones', () => {
    teamService.getStandings.and.returnValue(of(standings.slice(0, 5)));

    const rows = create().componentInstance.rows();

    expect(rows.filter(r => r.zone === 'accent').map(r => r.pos)).toEqual([1, 2, 3]);
    expect(rows.some(r => r.zone === 'danger')).toBeFalse();
  });

  it('derives zones and filter labels from the configured edge size', () => {
    edgeTeams.set(2);
    const component = create().componentInstance;

    const rows = component.rows();
    expect(rows.filter(r => r.zone === 'accent').map(r => r.pos)).toEqual([1, 2]);
    expect(rows.filter(r => r.zone === 'danger').map(r => r.pos)).toEqual([7, 8]);
    expect(component.filterOptions().map(o => o.label)).toEqual(['All', 'Top 4', 'Bottom 2']);
  });

  it('formats the goal difference with an explicit sign', () => {
    const component = create().componentInstance;
    const rows = component.rows();
    const team1 = rows.find(r => r.team.id === 1)!; // GD +2
    const team2 = rows.find(r => r.team.id === 2)!; // GD -2
    const team3 = rows.find(r => r.team.id === 3)!; // GD 0

    expect(component.formatGoalDiff(team1)).toBe('+2');
    expect(component.formatGoalDiff(team2)).toBe('-2');
    expect(component.formatGoalDiff(team3)).toBe('0');
  });

  it('filters the visible rows by zone segment', () => {
    const component = create().componentInstance;

    expect(component.visibleRows().length).toBe(8);

    component.filter.set('top');
    expect(component.visibleRows().map(r => r.pos)).toEqual([1, 2, 3, 4, 5, 6]);

    component.filter.set('bottom');
    expect(component.visibleRows().map(r => r.pos)).toEqual([6, 7, 8]);
  });

  it('reports the latest queue with a played match, or null before the season starts', () => {
    expect(create().componentInstance.latestPlayedQueue()).toBe(2);

    matchService.findAll.and.returnValue(of([matches[2]]));
    expect(create().componentInstance.latestPlayedQueue()).toBeNull();
  });
});

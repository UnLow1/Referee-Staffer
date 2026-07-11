import {ComponentFixture, TestBed} from '@angular/core/testing';
import {of} from 'rxjs';
import {StandingsComponent} from './standings.component';
import {TeamService} from '../../service/team.service';
import {Standing, Standings} from '../../model/standing';

describe('StandingsComponent', () => {
  let teamService: jasmine.SpyObj<TeamService>;

  // /api/teams/standings returns the computed table; 8 teams so both edge zones exist.
  const rows: Standing[] = Array.from({length: 8}, (_, i) => ({
    id: i + 1, name: `Team ${i + 1}`, city: `City ${i + 1}`, points: 40 - i * 5,
    place: i + 1, played: 10, wins: 8 - i, draws: i, losses: 2,
    goalsFor: 20 - i, goalsAgainst: 10 + i
  }));

  const standings: Standings = {afterQueue: 10, rows};

  beforeEach(async () => {
    teamService = jasmine.createSpyObj('TeamService', ['getStandings']);
    teamService.getStandings.and.returnValue(of(standings));

    await TestBed.configureTestingModule({
      imports: [StandingsComponent],
      providers: [
        {provide: TeamService, useValue: teamService}
      ]
    }).compileComponents();
  });

  function create(): ComponentFixture<StandingsComponent> {
    const fixture = TestBed.createComponent(StandingsComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('renders the backend-computed stats without local derivation', () => {
    const componentRows = create().componentInstance.rows();
    const first = componentRows.find(r => r.standing.id === 1)!;

    expect(componentRows.length).toBe(8);
    expect(first.standing).toEqual(jasmine.objectContaining(
      {place: 1, played: 10, wins: 8, draws: 0, losses: 2, goalsFor: 20, goalsAgainst: 10}));
  });

  it('marks the top three accent and the bottom three danger', () => {
    const componentRows = create().componentInstance.rows();

    expect(componentRows.filter(r => r.zone === 'accent').map(r => r.standing.place)).toEqual([1, 2, 3]);
    expect(componentRows.filter(r => r.zone === 'danger').map(r => r.standing.place)).toEqual([6, 7, 8]);
    expect(componentRows.filter(r => r.zone === null).map(r => r.standing.place)).toEqual([4, 5]);
  });

  it('suppresses the danger zone in a league too small for both zones', () => {
    teamService.getStandings.and.returnValue(of({afterQueue: 10, rows: rows.slice(0, 5)}));

    const componentRows = create().componentInstance.rows();

    expect(componentRows.filter(r => r.zone === 'accent').map(r => r.standing.place)).toEqual([1, 2, 3]);
    expect(componentRows.some(r => r.zone === 'danger')).toBeFalse();
  });

  it('formats the goal difference with an explicit sign', () => {
    const component = create().componentInstance;
    const componentRows = component.rows();
    const positive = componentRows.find(r => r.standing.id === 1)!; // GD +10
    const negative = componentRows.find(r => r.standing.id === 8)!; // GD -4
    const zero = componentRows.find(r => r.standing.id === 6)!; // 15:15

    expect(component.formatGoalDiff(positive)).toBe('+10');
    expect(component.formatGoalDiff(negative)).toBe('-4');
    expect(component.formatGoalDiff(zero)).toBe('0');
  });

  it('filters the visible rows by zone segment', () => {
    const component = create().componentInstance;

    expect(component.visibleRows().length).toBe(8);

    component.filter.set('top');
    expect(component.visibleRows().map(r => r.standing.place)).toEqual([1, 2, 3, 4, 5, 6]);

    component.filter.set('bottom');
    expect(component.visibleRows().map(r => r.standing.place)).toEqual([6, 7, 8]);
  });

  it('shows the backend afterQueue in the subtitle, or nothing before the season starts', () => {
    const fixture = create();
    expect(fixture.componentInstance.afterQueue()).toBe(10);
    expect((fixture.nativeElement as HTMLElement).querySelector('.page-head__sub')?.textContent)
      .toContain('after queue 10');

    teamService.getStandings.and.returnValue(of({afterQueue: null, rows: []}));
    const emptyFixture = create();
    expect(emptyFixture.componentInstance.afterQueue()).toBeNull();
    expect((emptyFixture.nativeElement as HTMLElement).querySelector('.page-head__sub')?.textContent)
      .not.toContain('after queue');
  });
});

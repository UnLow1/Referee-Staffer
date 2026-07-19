import {ComponentFixture, TestBed, fakeAsync, tick} from '@angular/core/testing';
import {Router} from '@angular/router';
import {of} from 'rxjs';
import {GradeListComponent} from './grade-list.component';
import {MatchService} from '../../service/match.service';
import {RefereeService} from '../../service/referee.service';
import {TeamService} from '../../service/team.service';
import {GradeService} from '../../service/grade.service';
import {Match} from '../../model/match';
import {Referee} from '../../model/referee';
import {Team} from '../../model/team';
import {Grade} from '../../model/grade';

describe('GradeListComponent', () => {
  let matchService: jasmine.SpyObj<MatchService>;
  let refereeService: jasmine.SpyObj<RefereeService>;
  let teamService: jasmine.SpyObj<TeamService>;
  let gradeService: jasmine.SpyObj<GradeService>;
  let router: jasmine.SpyObj<Router>;

  const teams: Team[] = [
    {id: 1, name: 'Alfa', city: 'Krakow', points: 40, short: 'ALF'},
    // No short code — shortCode falls back to the name prefix.
    {id: 2, name: 'Beta', city: 'Gdansk', points: 30},
    {id: 3, name: 'Gamma', city: 'Poznan', points: 20}
  ];
  const referees: Referee[] = [
    {id: 100, firstName: 'Jan', lastName: 'Kowalski', email: 'jan@example.com', experience: 10}
  ];
  const grades: Grade[] = [{id: 500, value: 8.4}, {id: 501, value: 6.5}];

  function makeMatch(id: number, overrides: Partial<Match> = {}): Match {
    return {
      id,
      queue: 1,
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

  const matches: Match[] = [
    makeMatch(11, {queue: 1, gradeId: 500, homeScore: 2, awayScore: 1}),
    makeMatch(12, {queue: 3, homeTeamId: 2, awayTeamId: 3, gradeId: 501}),
    // No grade — never becomes a row.
    makeMatch(13, {queue: 2}),
    // References a grade the backend no longer returns — dropped from the rows.
    makeMatch(14, {queue: 4, gradeId: 502})
  ];

  beforeEach(async () => {
    matchService = jasmine.createSpyObj('MatchService', ['findAll']);
    refereeService = jasmine.createSpyObj('RefereeService', ['findByIds']);
    teamService = jasmine.createSpyObj('TeamService', ['findByIds']);
    gradeService = jasmine.createSpyObj('GradeService', ['findByIds', 'delete']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    matchService.findAll.and.returnValue(of(matches));
    teamService.findByIds.and.returnValue(of(teams));
    refereeService.findByIds.and.returnValue(of(referees));
    gradeService.findByIds.and.returnValue(of(grades));

    await TestBed.configureTestingModule({
      imports: [GradeListComponent],
      providers: [
        {provide: MatchService, useValue: matchService},
        {provide: RefereeService, useValue: refereeService},
        {provide: TeamService, useValue: teamService},
        {provide: GradeService, useValue: gradeService},
        {provide: Router, useValue: router}
      ]
    }).compileComponents();
  });

  function create(): ComponentFixture<GradeListComponent> {
    const fixture = TestBed.createComponent(GradeListComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('builds rows only for matches whose grade resolved, newest queue first', () => {
    const component = create().componentInstance;

    expect(teamService.findByIds).toHaveBeenCalledWith([1, 2, 3]);
    expect(refereeService.findByIds).toHaveBeenCalledWith([100]);
    expect(gradeService.findByIds).toHaveBeenCalledWith([500, 501, 502]);

    const rows = component.rows();
    expect(rows.map(r => r.match.id)).toEqual([12, 11]);
    expect(rows[1].referee?.lastName).toBe('Kowalski');
    expect(rows[1].home?.name).toBe('Alfa');
    expect(rows[1].away?.name).toBe('Beta');
    expect(rows[1].grade.value).toBe(8.4);
  });

  it('skips the referee and grade lookups when nothing references them', fakeAsync(() => {
    matchService.findAll.and.returnValue(of([
      makeMatch(21, {refereeId: undefined}),
      makeMatch(22, {refereeId: undefined, homeTeamId: 2, awayTeamId: 3})
    ]));

    const component = create().componentInstance;
    // The empty-id branches resolve through Promise.resolve — flush the microtasks.
    tick();

    expect(refereeService.findByIds).not.toHaveBeenCalled();
    expect(gradeService.findByIds).not.toHaveBeenCalled();
    expect(component.rows()).toEqual([]);
    expect(component.matches().length).toBe(2);
  }));

  it('falls back to a derived short code when the team has none', () => {
    const component = create().componentInstance;

    expect(component.shortCode(teams[0])).toBe('ALF');
    expect(component.shortCode(teams[1])).toBe('BET');
    expect(component.shortCode(undefined)).toBe('');
  });

  it('scales grades to the meter and flags anything below seven', () => {
    const component = create().componentInstance;

    expect(component.gradeAsMeter(grades[0])).toBe(84);
    expect(component.gradeKind(grades[0])).toBe('default');
    expect(component.gradeKind(grades[1])).toBe('warn');
  });

  it('routes grade editing to the match form deep-link', () => {
    const component = create().componentInstance;
    const event = new Event('click');
    spyOn(event, 'stopPropagation');

    component.editGrade(component.rows()[1], event);

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/addMatch', 11]);
  });

  it('removes the row and detaches the grade from its match on delete', () => {
    gradeService.delete.and.returnValue(of(void 0));
    const component = create().componentInstance;
    const row = component.rows()[1];

    component.deleteGrade(row, new Event('click'));

    expect(gradeService.delete).toHaveBeenCalledWith(row.grade);
    expect(component.rows().map(r => r.match.id)).toEqual([12]);
    expect(component.matches().find(m => m.id === 11)?.gradeId).toBeUndefined();
  });
});

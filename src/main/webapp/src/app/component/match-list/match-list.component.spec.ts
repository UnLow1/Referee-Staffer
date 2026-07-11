import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, Router} from '@angular/router';
import {of} from 'rxjs';
import {MatchListComponent} from './match-list.component';
import {MatchService} from '../../service/match.service';
import {TeamService} from '../../service/team.service';
import {RefereeService} from '../../service/referee.service';
import {GradeService} from '../../service/grade.service';
import {Match} from '../../model/match';
import {Team} from '../../model/team';
import {Referee} from '../../model/referee';
import {Grade} from '../../model/grade';

describe('MatchListComponent', () => {
  let matchService: jasmine.SpyObj<MatchService>;
  let teamService: jasmine.SpyObj<TeamService>;
  let refereeService: jasmine.SpyObj<RefereeService>;
  let gradeService: jasmine.SpyObj<GradeService>;
  let router: jasmine.SpyObj<Router>;

  const teams: Team[] = [
    {id: 1, name: 'Alfa', city: 'Krakow', points: 40},
    {id: 2, name: 'Beta', city: 'Gdansk', points: 30},
    {id: 3, name: 'Gamma', city: 'Poznan', points: 20}
  ];
  const referees: Referee[] = [
    {id: 100, firstName: 'Jan', lastName: 'Kowalski', email: 'jan@example.com', experience: 10}
  ];
  const grades: Grade[] = [{id: 500, value: 8.4}];

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
    makeMatch(11, {queue: 1, refereeId: 100, gradeId: 500, homeScore: 2, awayScore: 1}),
    makeMatch(12, {queue: 2, homeTeamId: 2, awayTeamId: 3}),
    makeMatch(13, {queue: 2, homeTeamId: 3, awayTeamId: 1})
  ];

  beforeEach(() => {
    matchService = jasmine.createSpyObj('MatchService', ['findAll', 'findById', 'delete']);
    teamService = jasmine.createSpyObj('TeamService', ['findByIds', 'findAll']);
    refereeService = jasmine.createSpyObj('RefereeService', ['findByIds', 'findAll']);
    gradeService = jasmine.createSpyObj('GradeService', ['findByIds', 'findById']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    matchService.findAll.and.returnValue(of(matches));
    teamService.findByIds.and.returnValue(of(teams));
    refereeService.findByIds.and.returnValue(of(referees));
    gradeService.findByIds.and.returnValue(of(grades));
    // The match form (rendered when the drawer opens) loads these on init.
    teamService.findAll.and.returnValue(of(teams));
    refereeService.findAll.and.returnValue(of(referees));
    gradeService.findById.and.returnValue(of(grades[0]));
  });

  async function create(path = 'matches', id?: number): Promise<ComponentFixture<MatchListComponent>> {
    await TestBed.configureTestingModule({
      imports: [MatchListComponent],
      providers: [
        {provide: MatchService, useValue: matchService},
        {provide: TeamService, useValue: teamService},
        {provide: RefereeService, useValue: refereeService},
        {provide: GradeService, useValue: gradeService},
        {provide: Router, useValue: router},
        {
          provide: ActivatedRoute,
          useValue: {snapshot: {url: [{path}], paramMap: convertToParamMap(id ? {id: String(id)} : {})}}
        }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(MatchListComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('joins matches with teams, referees and grades by the ids actually referenced', async () => {
    const component = (await create()).componentInstance;

    expect(teamService.findByIds).toHaveBeenCalledWith([1, 2, 3]);
    expect(refereeService.findByIds).toHaveBeenCalledWith([100]);
    expect(gradeService.findByIds).toHaveBeenCalledWith([500]);
    expect(component.getTeam(1)?.name).toBe('Alfa');
    expect(component.getReferee(100)?.lastName).toBe('Kowalski');
    expect(component.getGrade(500)?.value).toBe(8.4);
  });

  it('skips the referee and grade lookups when nothing references them', async () => {
    matchService.findAll.and.returnValue(of([makeMatch(21), makeMatch(22, {homeTeamId: 2, awayTeamId: 3})]));

    const component = (await create()).componentInstance;

    expect(refereeService.findByIds).not.toHaveBeenCalled();
    expect(gradeService.findByIds).not.toHaveBeenCalled();
    expect(component.matches().length).toBe(2);
  });

  it('defaults to the latest queue and lists queues descending', async () => {
    const component = (await create()).componentInstance;

    expect(component.availableQueues()).toEqual([2, 1]);
    expect(component.selectedQueue()).toBe(2);
  });

  it('filters by selected queue and team-name search', async () => {
    const component = (await create()).componentInstance;

    expect(component.visibleMatches().map(m => m.id)).toEqual([12, 13]);

    component.selectQueue(1);
    expect(component.visibleMatches().map(m => m.id)).toEqual([11]);

    component.selectQueue(2);
    component.setSearch('gamma');
    expect(component.visibleMatches().map(m => m.id)).toEqual([12, 13]);

    component.setSearch('alfa');
    expect(component.visibleMatches().map(m => m.id)).toEqual([13]);
  });

  it('filters by result and referee assignment across all queues', async () => {
    const component = (await create()).componentInstance;
    component.selectQueue(null);

    component.resultFilter.set('played');
    expect(component.visibleMatches().map(m => m.id)).toEqual([11]);

    component.resultFilter.set('unplayed');
    expect(component.visibleMatches().map(m => m.id)).toEqual([12, 13]);

    component.resultFilter.set('all');
    component.refereeFilter.set('assigned');
    expect(component.visibleMatches().map(m => m.id)).toEqual([11]);

    component.refereeFilter.set('unassigned');
    expect(component.visibleMatches().map(m => m.id)).toEqual([12, 13]);
  });

  it('combines status filters with the queue and the team search', async () => {
    const component = (await create()).componentInstance;

    component.selectQueue(2);
    component.refereeFilter.set('unassigned');
    component.setSearch('gamma');
    expect(component.visibleMatches().map(m => m.id)).toEqual([12, 13]);

    component.resultFilter.set('played');
    expect(component.visibleMatches()).toEqual([]);
  });

  it('treats a half-recorded score as not played', async () => {
    matchService.findAll.and.returnValue(of([makeMatch(31, {homeScore: 1})]));

    const component = (await create()).componentInstance;
    component.resultFilter.set('unplayed');

    expect(component.visibleMatches().map(m => m.id)).toEqual([31]);
  });

  it('counts active filters and clears them in one go', async () => {
    const component = (await create()).componentInstance;
    expect(component.activeFilterCount()).toBe(0);

    component.resultFilter.set('played');
    component.refereeFilter.set('assigned');
    expect(component.activeFilterCount()).toBe(2);

    component.clearFilters();
    expect(component.activeFilterCount()).toBe(0);
    expect(component.resultFilter()).toBe('all');
    expect(component.refereeFilter()).toBe('all');
  });

  it('toggles the filter bar', async () => {
    const component = (await create()).componentInstance;
    expect(component.filtersOpen()).toBeFalse();

    component.toggleFilters();
    expect(component.filtersOpen()).toBeTrue();

    component.toggleFilters();
    expect(component.filtersOpen()).toBeFalse();
  });

  it('renders the score only when both halves are present', async () => {
    const component = (await create()).componentInstance;

    expect(component.scoreOrPlaceholder(matches[0])).toBe('2 – 1');
    expect(component.scoreOrPlaceholder(matches[1])).toBeNull();
    expect(component.scoreOrPlaceholder(makeMatch(31, {homeScore: 1}))).toBeNull();
  });

  it('scales a 1..10 grade to the 0..100 meter', async () => {
    const component = (await create()).componentInstance;

    expect(component.gradeAsMeter(grades[0])).toBe(84);
    expect(component.gradeAsMeter(undefined)).toBe(0);
  });

  it('deletes only after the confirm, labelling the fixture in the guard', async () => {
    const component = (await create()).componentInstance;
    matchService.delete.and.returnValue(of(void 0));

    component.askDelete(matches[0], new Event('click'));
    expect(component.deleteGuard().message).toContain('Alfa – Beta');
    expect(matchService.delete).not.toHaveBeenCalled();

    component.confirmDelete();

    expect(matchService.delete).toHaveBeenCalledWith(11);
    expect(component.matches().map(m => m.id)).toEqual([12, 13]);
    expect(component.deleteTarget()).toBeNull();
  });

  describe('deep links', () => {
    it('opens an empty drawer for /addMatch', async () => {
      const component = (await create('addMatch')).componentInstance;

      expect(component.formOpen()).toBeTrue();
      expect(component.editingMatch()).toBeNull();
    });

    it('fetches the match and opens the edit drawer for /addMatch/:id', async () => {
      matchService.findById.and.returnValue(of(matches[0]));

      const component = (await create('addMatch', 11)).componentInstance;

      expect(matchService.findById).toHaveBeenCalledWith(11);
      expect(component.editingMatch()).toEqual(matches[0]);
      expect(component.formOpen()).toBeTrue();
    });

    it('normalizes the URL back to the list on close only when deep-linked', async () => {
      const deepLinked = (await create('addMatch')).componentInstance;
      deepLinked.closeForm();
      expect(router.navigate).toHaveBeenCalledWith(['/matches']);
    });
  });
});

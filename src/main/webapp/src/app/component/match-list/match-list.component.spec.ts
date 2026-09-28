import type {MockedObject} from 'vitest';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, Router} from '@angular/router';
import {of} from 'rxjs';
import {MatchListComponent, centerScrollLeft} from './match-list.component';
import {MatchService} from '../../service/match.service';
import {TeamService} from '../../service/team.service';
import {RefereeService} from '../../service/referee.service';
import {GradeService} from '../../service/grade.service';
import {Match} from '../../model/match';
import {Team} from '../../model/team';
import {Referee} from '../../model/referee';
import {Grade} from '../../model/grade';
import {createMock} from '../../testing/mock';

describe('MatchListComponent', () => {
  let matchService: MockedObject<MatchService>;
  let teamService: MockedObject<TeamService>;
  let refereeService: MockedObject<RefereeService>;
  let gradeService: MockedObject<GradeService>;
  let router: MockedObject<Router>;

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
    matchService = createMock<MatchService>(['findAll', 'findById', 'delete']);
    teamService = createMock<TeamService>(['findByIds', 'findAll']);
    refereeService = createMock<RefereeService>(['findByIds', 'findAll']);
    gradeService = createMock<GradeService>(['findByIds', 'findById']);
    router = createMock<Router>(['navigate']);

    matchService.findAll.mockReturnValue(of(matches));
    teamService.findByIds.mockReturnValue(of(teams));
    refereeService.findByIds.mockReturnValue(of(referees));
    gradeService.findByIds.mockReturnValue(of(grades));
    // The match form (rendered when the drawer opens) loads these on init.
    teamService.findAll.mockReturnValue(of(teams));
    refereeService.findAll.mockReturnValue(of(referees));
    gradeService.findById.mockReturnValue(of(grades[0]));
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
    matchService.findAll.mockReturnValue(of([makeMatch(21), makeMatch(22, {homeTeamId: 2, awayTeamId: 3})]));

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

  it('keeps an explicit "All queues" choice across the reload after a drawer save', async () => {
    const component = (await create()).componentInstance;
    expect(component.selectedQueue()).toBe(2);

    component.selectQueue(null);
    component.onSaved();

    expect(component.selectedQueue()).toBeNull();
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
    matchService.findAll.mockReturnValue(of([makeMatch(31, {homeScore: 1})]));

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
    expect(component.filtersOpen()).toBe(false);

    component.toggleFilters();
    expect(component.filtersOpen()).toBe(true);

    component.toggleFilters();
    expect(component.filtersOpen()).toBe(false);
  });

  describe('pagination', () => {
    // 60 matches across two queues (30 each) — enough for three 25-item pages on "All queues".
    const manyMatches: Match[] = Array.from({length: 60}, (_, i) =>
      makeMatch(1000 + i, {queue: i < 30 ? 1 : 2}));

    beforeEach(() => {
      matchService.findAll.mockReturnValue(of(manyMatches));
    });

    it('slices the visible list to the current page', async () => {
      const component = (await create()).componentInstance;
      component.selectQueue(null);

      expect(component.visibleMatches().length).toBe(60);
      expect(component.pagedMatches().length).toBe(25);
      expect(component.pagedMatches()[0].id).toBe(1000);

      component.setPage(3);
      expect(component.currentPage()).toBe(3);
      expect(component.pagedMatches().map(m => m.id)).toEqual([1050, 1051, 1052, 1053, 1054,
        1055, 1056, 1057, 1058, 1059]);
    });

    it('resets to the first page when the queue, search or status filters change', async () => {
      const component = (await create()).componentInstance;
      component.selectQueue(null);
      component.setPage(3);

      component.selectQueue(1);
      expect(component.page()).toBe(1);

      component.setPage(2);
      component.setSearch('alfa');
      expect(component.page()).toBe(1);

      component.setSearch('');
      component.setPage(2);
      component.setResultFilter('unplayed');
      expect(component.page()).toBe(1);

      component.setPage(2);
      component.setRefereeFilter('unassigned');
      expect(component.page()).toBe(1);

      component.setPage(2);
      component.clearFilters();
      expect(component.page()).toBe(1);
    });

    it('clamps the page when the filtered list shrinks below it', async () => {
      const component = (await create()).componentInstance;
      component.selectQueue(null);
      component.setPage(3);

      // Queue 1 alone has 30 matches → 2 pages; the requested page 3 must clamp to 2.
      component.selectedQueue.set(1);

      expect(component.currentPage()).toBe(2);
      expect(component.pagedMatches().length).toBe(5);
    });
  });

  // ——— RS-113: a full season renders 30+ queue buttons, so the bar has to scroll ———
  describe('queue bar', () => {
    /**
     * jsdom has no layout, so offsets have to be declared. Builds a bar of `count`
     * queue buttons, each `buttonWidth` wide, inside a viewport of `viewportWidth`.
     */
    function makeBar(count: number, buttonWidth: number, viewportWidth: number): HTMLElement {
      const bar = document.createElement('div');
      bar.innerHTML = `<button data-queue="all"></button>` +
        Array.from({length: count}, (_, i) => `<button data-queue="${i + 1}"></button>`).join('');
      bar.querySelectorAll('button').forEach((button, i) => {
        Object.defineProperty(button, 'offsetLeft', {value: i * buttonWidth});
        Object.defineProperty(button, 'offsetWidth', {value: buttonWidth});
      });
      Object.defineProperty(bar, 'clientWidth', {value: viewportWidth});
      Object.defineProperty(bar, 'scrollWidth', {value: (count + 1) * buttonWidth});
      return bar;
    }

    it('wraps the queue bar in a scroll container and tags every button with its queue', async () => {
      const fixture = await create();
      const host: HTMLElement = fixture.nativeElement;

      const scroller = host.querySelector('.seg-scroll');
      expect(scroller).not.toBeNull();
      expect(scroller!.querySelector('.seg')).not.toBeNull();
      expect([...host.querySelectorAll('.seg-scroll [data-queue]')].map(b => b.getAttribute('data-queue')))
        .toEqual(['all', '2', '1']);
      // The counter must survive next to a bar wide enough to overflow the panel head.
      expect(host.querySelector('.queue-count')?.textContent).toContain('2 matches');
    });

    it('centers the selected queue inside the scrollable bar', async () => {
      const component = (await create()).componentInstance;
      // Queue 25 sits at offset 2500 in a 3100px bar shown through a 300px viewport:
      // centering it means 2500 + 50 - 150.
      const bar = makeBar(30, 100, 300);

      expect(component.scrollQueueIntoView(bar, 25)).toBe(2400);
    });

    it('clamps the scroll to the bar ends so the first and last queue stay flush', async () => {
      const component = (await create()).componentInstance;
      const bar = makeBar(30, 100, 300);

      expect(component.scrollQueueIntoView(bar, null)).toBe(0);
      expect(component.scrollQueueIntoView(bar, 1)).toBe(0);
      // Last button ends at the bar's right edge → 3100 - 300.
      expect(component.scrollQueueIntoView(bar, 30)).toBe(2800);
    });

    it('does nothing when the queue has no button', async () => {
      const component = (await create()).componentInstance;
      const bar = makeBar(3, 100, 300);

      expect(component.scrollQueueIntoView(bar, 99)).toBeNull();
    });

    it('leaves a bar that already fits unscrolled', async () => {
      const component = (await create()).componentInstance;
      const bar = makeBar(2, 100, 800);

      expect(component.scrollQueueIntoView(bar, 2)).toBe(0);
    });
  });

  describe('centerScrollLeft', () => {
    it('centers the item in the viewport', () => {
      expect(centerScrollLeft(500, 100, 300, 2000)).toBe(400);
    });

    it('never scrolls past either end', () => {
      expect(centerScrollLeft(0, 100, 300, 2000)).toBe(0);
      expect(centerScrollLeft(1900, 100, 300, 2000)).toBe(1700);
    });

    it('returns 0 when the content is not wider than the viewport', () => {
      expect(centerScrollLeft(100, 100, 800, 400)).toBe(0);
    });
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
    matchService.delete.mockReturnValue(of(void 0));

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

      expect(component.formOpen()).toBe(true);
      expect(component.editingMatch()).toBeNull();
    });

    it('fetches the match and opens the edit drawer for /addMatch/:id', async () => {
      matchService.findById.mockReturnValue(of(matches[0]));

      const component = (await create('addMatch', 11)).componentInstance;

      expect(matchService.findById).toHaveBeenCalledWith(11);
      expect(component.editingMatch()).toEqual(matches[0]);
      expect(component.formOpen()).toBe(true);
    });

    it('normalizes the URL back to the list on close only when deep-linked', async () => {
      const deepLinked = (await create('addMatch')).componentInstance;
      deepLinked.closeForm();
      expect(router.navigate).toHaveBeenCalledWith(['/matches']);
    });
  });
});

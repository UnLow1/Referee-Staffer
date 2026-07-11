import {ComponentFixture, TestBed} from '@angular/core/testing';
import {signal, WritableSignal} from '@angular/core';
import {of, Subject, throwError} from 'rxjs';
import {StafferComponent} from './staffer.component';
import {StafferService} from '../../service/staffer.service';
import {TeamService} from '../../service/team.service';
import {RefereeService} from '../../service/referee.service';
import {MatchService} from '../../service/match.service';
import {UiSettingsService} from '../../service/ui-settings.service';
import {Match} from '../../model/match';
import {Standings} from '../../model/standing';
import {Referee} from '../../model/referee';
import {DifficultyBreakdown} from '../../model/difficultyBreakdown';

describe('StafferComponent', () => {
  let fixture: ComponentFixture<StafferComponent>;
  let component: StafferComponent;
  let stafferService: jasmine.SpyObj<StafferService>;
  let teamService: jasmine.SpyObj<TeamService>;
  let refereeService: jasmine.SpyObj<RefereeService>;
  let matchService: jasmine.SpyObj<MatchService>;
  let explainerVisible: WritableSignal<boolean>;

  // Backend rows carry `place`. 8 teams, NUMBER_OF_EDGE_TEAMS = 3, so top = both
  // places <= 3, bottom = both places > 5. Teams 1 and 2 share a city.
  const standings: Standings = {
    afterQueue: 10,
    rows: [
      {id: 1, name: 'Alfa', city: 'Krakow', points: 40},
      {id: 2, name: 'Beta', city: 'Krakow', points: 35},
      {id: 3, name: 'Gamma', city: 'Gdansk', points: 30},
      {id: 4, name: 'Delta', city: 'Poznan', points: 25},
      {id: 5, name: 'Epsilon', city: 'Lodz', points: 20},
      {id: 6, name: 'Zeta', city: 'Wroclaw', points: 15},
      {id: 7, name: 'Eta', city: 'Radom', points: 10},
      {id: 8, name: 'Theta', city: 'Opole', points: 5}
    ].map((team, i) => ({
      ...team, place: i + 1, played: 10, wins: 8 - i, draws: i, losses: 2,
      goalsFor: 20 - i, goalsAgainst: 10 + i
    }))
  };

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

  function makeReferee(id: number, overrides: Partial<Referee> = {}): Referee {
    return {
      id,
      firstName: `First${id}`,
      lastName: `Last${id}`,
      email: `ref${id}@example.com`,
      experience: 10,
      ...overrides
    };
  }

  const matches: Match[] = [
    // Derby + top-3 (places 1 and 2, same city), hardest.
    makeMatch(11, {homeTeamId: 1, awayTeamId: 2, refereeId: 100, hardnessLvl: 120.4}),
    // Relegation (places 7 and 8), unassigned.
    makeMatch(12, {homeTeamId: 7, awayTeamId: 8, hardnessLvl: 80.2}),
    // Mid-table, no flags.
    makeMatch(13, {homeTeamId: 4, awayTeamId: 5, refereeId: 101, hardnessLvl: 50})
  ];

  const referees: Referee[] = [
    makeReferee(100, {potential: 90, experience: 10}),
    makeReferee(101, {potential: 70, experience: 20}),
    makeReferee(102, {experience: 5}), // un-enriched: no potential, sorts by experience
    makeReferee(103, {potential: 95, experience: 15})
  ];

  function makeBreakdown(matchId: number): DifficultyBreakdown {
    return {
      matchId,
      total: 120.4,
      parts: {base: 90.4, sameCity: 15, top: 15, bottom: 0},
      flags: {sameCity: true, isTop: true, isBot: false, pointsDiff: 5}
    };
  }

  beforeEach(async () => {
    stafferService = jasmine.createSpyObj('StafferService', ['staffReferees']);
    teamService = jasmine.createSpyObj('TeamService', ['getStandings']);
    refereeService = jasmine.createSpyObj('RefereeService', ['findRefereesAvailableForQueue']);
    matchService = jasmine.createSpyObj('MatchService', ['getDifficultyBreakdown', 'updateList']);
    explainerVisible = signal(false);

    stafferService.staffReferees.and.returnValue(of(matches));
    teamService.getStandings.and.returnValue(of(standings));
    refereeService.findRefereesAvailableForQueue.and.returnValue(of(referees));
    matchService.getDifficultyBreakdown.and.callFake(id => of(makeBreakdown(id)));
    matchService.updateList.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [StafferComponent],
      providers: [
        {provide: StafferService, useValue: stafferService},
        {provide: TeamService, useValue: teamService},
        {provide: RefereeService, useValue: refereeService},
        {provide: MatchService, useValue: matchService},
        {provide: UiSettingsService, useValue: {explainerVisible: explainerVisible.asReadonly()}}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StafferComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('queue stepper', () => {
    it('increments and decrements the queue', () => {
      component.incQueue();
      component.incQueue();
      expect(component.queue()).toBe(3);
      component.decQueue();
      expect(component.queue()).toBe(2);
    });

    it('does not go below queue 1', () => {
      component.decQueue();
      component.decQueue();
      expect(component.queue()).toBe(1);
    });
  });

  describe('generate', () => {
    it('staffs the selected queue and populates matches, referees and standings lookups', () => {
      component.incQueue();
      component.generate();

      expect(stafferService.staffReferees).toHaveBeenCalledWith(2);
      expect(refereeService.findRefereesAvailableForQueue).toHaveBeenCalledWith(2);
      expect(component.matches()).toEqual(matches);
      expect(component.referees()).toEqual(referees);
      expect(component.totalTeams()).toBe(8);
      expect(component.getTeam(1)?.name).toBe('Alfa');
      expect(component.getTeam(999)).toBeUndefined();
    });

    it('keeps loading true until the forkJoin completes and clears it on success', () => {
      const staffSubject = new Subject<Match[]>();
      stafferService.staffReferees.and.returnValue(staffSubject);

      component.generate();
      expect(component.loading()).toBeTrue();

      staffSubject.next(matches);
      staffSubject.complete();
      expect(component.loading()).toBeFalse();
    });

    it('clears loading and leaves matches untouched on error', () => {
      stafferService.staffReferees.and.returnValue(throwError(() => new Error('boom')));

      component.generate();

      expect(component.loading()).toBeFalse();
      expect(component.matches()).toBeNull();
    });

    it('resets the saved-at marker so a stale "Saved" note never shows for a new cast', () => {
      component.generate();
      component.save();
      expect(component.savedAt()).not.toBeNull();

      component.generate();
      expect(component.savedAt()).toBeNull();
    });
  });

  describe('derived state', () => {
    beforeEach(() => component.generate());

    it('sorts matches by difficulty descending', () => {
      expect(component.sortedMatches().map(m => m.id)).toEqual([11, 12, 13]);
    });

    it('sums total difficulty rounded', () => {
      // 120.4 + 80.2 + 50 = 250.6 -> 251
      expect(component.totalDifficulty()).toBe(251);
    });
  });

  describe('locks', () => {
    beforeEach(() => component.generate());

    it('locks and unlocks an assigned match', () => {
      const assigned = component.matches()![0];

      component.toggleLock(assigned);
      expect(component.isLocked(assigned)).toBeTrue();
      expect(component.lockCount()).toBe(1);

      component.toggleLock(assigned);
      expect(component.isLocked(assigned)).toBeFalse();
      expect(component.lockCount()).toBe(0);
    });

    it('ignores locking a match without an assigned referee', () => {
      const unassigned = component.matches()!.find(m => m.id === 12)!;

      component.toggleLock(unassigned);

      expect(component.isLocked(unassigned)).toBeFalse();
      expect(component.lockCount()).toBe(0);
    });

    it('clears all locks at once', () => {
      component.toggleLock(component.matches()![0]);
      component.toggleLock(component.matches()![2]);
      expect(component.lockCount()).toBe(2);

      component.clearLocks();
      expect(component.lockCount()).toBe(0);
    });
  });

  describe('swap', () => {
    beforeEach(() => component.generate());

    it('reassigns the referee immutably and locks the match', () => {
      const before = component.matches()!;
      const target = before.find(m => m.id === 12)!;

      component.swap(target, 102);

      const after = component.matches()!;
      expect(after).not.toBe(before);
      expect(after.find(m => m.id === 12)!.refereeId).toBe(102);
      expect(before.find(m => m.id === 12)!.refereeId).toBeUndefined();
      expect(component.isLocked(target)).toBeTrue();
      expect(component.locks().get(12)).toBe(102);
    });

    it('invalidates the saved-at marker', () => {
      component.save();
      expect(component.savedAt()).not.toBeNull();

      component.swap(component.matches()![0], 103);
      expect(component.savedAt()).toBeNull();
    });
  });

  describe('drawer', () => {
    beforeEach(() => component.generate());

    it('opens with the clicked match and lazily loads its breakdown', () => {
      const target = component.matches()![0];

      component.openDrawer(target);

      expect(component.drawerMatch()).toEqual(target);
      expect(matchService.getDifficultyBreakdown).toHaveBeenCalledWith(11);
      expect(component.drawerBreakdown()?.matchId).toBe(11);
    });

    it('discards a stale breakdown response after switching to another match', () => {
      const first = new Subject<DifficultyBreakdown>();
      const second = new Subject<DifficultyBreakdown>();
      matchService.getDifficultyBreakdown.and.returnValues(first, second);
      const [m1, m2] = component.matches()!;

      component.openDrawer(m1);
      component.openDrawer(m2);

      first.next(makeBreakdown(m1.id));
      expect(component.drawerBreakdown()).toBeNull();

      second.next(makeBreakdown(m2.id));
      expect(component.drawerBreakdown()?.matchId).toBe(m2.id);
    });

    it('closes and resets the breakdown', () => {
      component.openDrawer(component.matches()![0]);

      component.closeDrawer();

      expect(component.drawerMatch()).toBeNull();
      expect(component.drawerBreakdown()).toBeNull();
    });
  });

  describe('candidatesFor', () => {
    beforeEach(() => component.generate());

    it('sorts by potential descending with experience fallback for un-enriched referees', () => {
      const target = component.matches()!.find(m => m.id === 12)!;

      const ids = component.candidatesFor(target).map(c => c.referee.id);

      // potentials: 103 -> 95, 100 -> 90, 101 -> 70; 102 falls back to experience 5.
      expect(ids).toEqual([103, 100, 101, 102]);
    });

    it('flags the assigned referee and referees used by other matches in the queue', () => {
      const target = component.matches()!.find(m => m.id === 11)!;

      const candidates = component.candidatesFor(target);
      const byId = new Map(candidates.map(c => [c.referee.id, c]));

      expect(byId.get(100)!.isAssigned).toBeTrue();
      expect(byId.get(100)!.isUsedElsewhere).toBeFalse();
      expect(byId.get(101)!.isUsedElsewhere).toBeTrue();
      expect(byId.get(102)!.isUsedElsewhere).toBeFalse();
      expect(byId.get(103)!.isUsedElsewhere).toBeFalse();
    });
  });

  describe('flags', () => {
    beforeEach(() => component.generate());

    it('marks a same-city top-3 pairing as derby and top', () => {
      const flags = component.flags(makeMatch(21, {homeTeamId: 1, awayTeamId: 2}));
      expect(flags).toEqual({sameCity: true, isTop: true, isBot: false});
    });

    it('marks a pairing of the bottom three as relegation', () => {
      const flags = component.flags(makeMatch(22, {homeTeamId: 7, awayTeamId: 8}));
      expect(flags).toEqual({sameCity: false, isTop: false, isBot: true});
    });

    it('requires both teams inside the edge zone', () => {
      // Places 3 vs 4: only home side is top-3. Places 5 vs 8: only away side is bottom-3.
      expect(component.flags(makeMatch(23, {homeTeamId: 3, awayTeamId: 4})).isTop).toBeFalse();
      expect(component.flags(makeMatch(24, {homeTeamId: 5, awayTeamId: 8})).isBot).toBeFalse();
    });

    it('treats the zone boundaries as inclusive', () => {
      // Places 1 vs 3 are both <= 3; places 6 vs 8 are both > 8 - 3.
      expect(component.flags(makeMatch(25, {homeTeamId: 1, awayTeamId: 3})).isTop).toBeTrue();
      expect(component.flags(makeMatch(26, {homeTeamId: 6, awayTeamId: 8})).isBot).toBeTrue();
    });

    it('reports no flags for a mid-table cross-city pairing', () => {
      expect(component.hasNoFlags(makeMatch(27, {homeTeamId: 4, awayTeamId: 5}))).toBeTrue();
    });

    it('degrades to no flags for teams missing from the standings', () => {
      expect(component.hasNoFlags(makeMatch(28, {homeTeamId: 998, awayTeamId: 999}))).toBeTrue();
    });
  });

  describe('save', () => {
    it('sends the current cast and stamps the save time', () => {
      component.generate();

      component.save();

      expect(matchService.updateList).toHaveBeenCalledWith(component.matches()!);
      expect(component.savedAt()).toBeInstanceOf(Date);
    });

    it('does nothing before a cast is generated', () => {
      component.save();

      expect(matchService.updateList).not.toHaveBeenCalled();
      expect(component.savedAt()).toBeNull();
    });
  });

  describe('difficultyKind', () => {
    it('warns from 100 upward and defaults below or when missing', () => {
      expect(component.difficultyKind(99.9)).toBe('default');
      expect(component.difficultyKind(100)).toBe('warn');
      expect(component.difficultyKind(null)).toBe('default');
      expect(component.difficultyKind(undefined)).toBe('default');
    });
  });

  describe('template', () => {
    it('shows the empty state until a cast is generated', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('.empty-state')).not.toBeNull();
      expect(el.querySelector('.panel--kpi-strip')).toBeNull();
    });

    it('renders the KPI strip and one row per match after generating', () => {
      const el: HTMLElement = fixture.nativeElement;
      (el.querySelector('.page-head__actions .btn--primary') as HTMLButtonElement).click();
      fixture.detectChanges();

      expect(el.querySelector('.empty-state')).toBeNull();
      expect(el.querySelector('.panel--kpi-strip')).not.toBeNull();
      expect(el.querySelectorAll('tr.cast-row').length).toBe(3);
    });

    it('gates the algorithm explainer on the UI setting', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('.explainer')).toBeNull();

      explainerVisible.set(true);
      fixture.detectChanges();

      expect(el.querySelector('.explainer')).not.toBeNull();
    });
  });
});

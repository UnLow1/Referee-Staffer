import type {MockedObject} from 'vitest';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {signal, WritableSignal} from '@angular/core';
import {of, Subject, throwError} from 'rxjs';
import {StafferComponent} from './staffer.component';
import {StafferService} from '../../service/staffer.service';
import {TeamService} from '../../service/team.service';
import {RefereeService} from '../../service/referee.service';
import {MatchService} from '../../service/match.service';
import {ConfigurationService} from '../../service/configuration.service';
import {UiSettingsService} from '../../service/ui-settings.service';
import {Match} from '../../model/match';
import {Standings} from '../../model/standing';
import {Referee} from '../../model/referee';
import {DifficultyBreakdown} from '../../model/difficultyBreakdown';
import {createMock} from '../../testing/mock';
import {SAVE_FILE} from '../../service/save-file.token';

describe('StafferComponent', () => {
  let fixture: ComponentFixture<StafferComponent>;
  let component: StafferComponent;
  let stafferService: MockedObject<StafferService>;
  let teamService: MockedObject<TeamService>;
  let refereeService: MockedObject<RefereeService>;
  let matchService: MockedObject<MatchService>;
  let explainerVisible: WritableSignal<boolean>;
  let edgeTeams: WritableSignal<number>;
  // The download is injected (SAVE_FILE) because jsdom has no download machinery; a fresh
  // spy per test replaces the module mock the Angular unit-test builder cannot honour.
  let saveFile: ReturnType<typeof vi.fn>;

  // Backend rows carry `place`. 8 teams, edge size 3 (stubbed ConfigurationService),
  // so top = both places <= 3, bottom = both places > 5. Teams 1 and 2 share a city.
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
    stafferService = createMock<StafferService>(['staffReferees', 'getStoredCast']);
    teamService = createMock<TeamService>(['getStandings']);
    refereeService = createMock<RefereeService>(['findRefereesAvailableForQueue']);
    matchService = createMock<MatchService>(['getDifficultyBreakdown', 'updateList', 'downloadAssignmentsPdf']);
    explainerVisible = signal(false);
    edgeTeams = signal(3);
    saveFile = vi.fn();

    stafferService.staffReferees.mockReturnValue(of(matches));
    // Default: nothing stored yet, so the screen starts on the empty state the way it did
    // before RS-105. Tests that need a saved cast override this.
    stafferService.getStoredCast.mockReturnValue(of([]));
    teamService.getStandings.mockReturnValue(of(standings));
    refereeService.findRefereesAvailableForQueue.mockReturnValue(of(referees));
    matchService.getDifficultyBreakdown.mockImplementation(id => of(makeBreakdown(id)));
    matchService.updateList.mockReturnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [StafferComponent],
      providers: [
        {provide: StafferService, useValue: stafferService},
        {provide: TeamService, useValue: teamService},
        {provide: RefereeService, useValue: refereeService},
        {provide: MatchService, useValue: matchService},
        {provide: ConfigurationService, useValue: {edgeTeams: edgeTeams.asReadonly(), ensureEdgeTeamsLoaded: vi.fn().mockName('ensureEdgeTeamsLoaded')}},
        {provide: UiSettingsService, useValue: {explainerVisible: explainerVisible.asReadonly()}},
        {provide: SAVE_FILE, useValue: saveFile}
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

    it('loads the stored cast of the queue it lands on', () => {
      stafferService.getStoredCast.mockClear();

      component.incQueue();

      expect(stafferService.getStoredCast).toHaveBeenCalledWith(2);
      expect(refereeService.findRefereesAvailableForQueue).toHaveBeenCalledWith(2);
      expect(stafferService.staffReferees).not.toHaveBeenCalled();
    });

    it('does not reload when the queue cannot go any lower', () => {
      stafferService.getStoredCast.mockClear();

      component.decQueue();

      expect(stafferService.getStoredCast).not.toHaveBeenCalled();
    });

    it('closes an open drawer — it belongs to the previous queue', () => {
      component.generate();
      component.openDrawer(component.matches()![0]);
      expect(component.drawerMatch()).not.toBeNull();

      component.incQueue();

      expect(component.drawerMatch()).toBeNull();
      expect(component.drawerBreakdown()).toBeNull();
    });
  });

  describe('stored cast', () => {
    it('loads what the backend has stored for queue 1 on entry', () => {
      // beforeEach already ran ngOnInit through the first detectChanges.
      expect(stafferService.getStoredCast).toHaveBeenCalledWith(1);
      expect(stafferService.staffReferees).not.toHaveBeenCalled();
      expect(component.matches()).toEqual([]);
      expect(component.persisted()).toBe(true);
    });

    it('shows the stored cast without regenerating it, and allows exporting it right away', () => {
      stafferService.getStoredCast.mockReturnValue(of(matches));

      component.loadStoredCast();

      expect(component.matches()).toEqual(matches);
      expect(component.persisted()).toBe(true);
      expect(component.canExport()).toBe(true);
      expect(stafferService.staffReferees).not.toHaveBeenCalled();
    });

    it('leaves no save marker — nothing records when a stored cast was saved', () => {
      stafferService.getStoredCast.mockReturnValue(of(matches));

      component.loadStoredCast();

      expect(component.savedAt()).toBeNull();
    });

    it('offers a retry when the cast could not be loaded', () => {
      stafferService.getStoredCast.mockReturnValue(throwError(() => new Error('boom')));

      component.loadStoredCast();

      expect(component.loadFailed()).toBe(true);
      expect(component.loading()).toBe(false);
      expect(component.canExport()).toBe(false);

      stafferService.getStoredCast.mockReturnValue(of(matches));
      component.loadStoredCast();

      expect(component.loadFailed()).toBe(false);
      expect(component.matches()).toEqual(matches);
    });

    it('ignores a failure from a queue the stepper has already left', () => {
      const abandoned = new Subject<Match[]>();
      const current = new Subject<Match[]>();
      stafferService.getStoredCast.mockReturnValueOnce(abandoned).mockReturnValueOnce(current);

      component.incQueue();
      component.incQueue();
      expect(component.loading()).toBe(true);

      abandoned.error(new Error('boom'));

      // The newer request still owns the loading state, and the screen must not claim the
      // queue it is now showing failed.
      expect(component.loading()).toBe(true);
      expect(component.loadFailed()).toBe(false);

      current.next(matches);
      current.complete();
      expect(component.loading()).toBe(false);
      expect(component.matches()).toEqual(matches);
    });

    it('ignores a response that lands after the queue moved on', () => {
      const slow = new Subject<Match[]>();
      stafferService.getStoredCast.mockReturnValueOnce(slow);

      component.incQueue();
      component.incQueue();
      slow.next(matches);
      slow.complete();

      expect(component.queue()).toBe(3);
      expect(component.matches()).toEqual([]);
    });
  });

  describe('queue change resets the cast', () => {
    beforeEach(() => {
      component.generate();
      component.save();
      component.openDrawer(component.matches()![0]);
    });

    it('drops every piece of state that belongs to the queue left behind', () => {
      component.toggleLock(component.matches()![2]);

      component.incQueue();

      // The rows on screen are queue 2's stored cast now (empty in this fixture), not queue 1's.
      expect(stafferService.getStoredCast).toHaveBeenLastCalledWith(2);
      expect(component.matches()).toEqual([]);
      // ...and so is the referee pool: it was refetched for queue 2.
      expect(refereeService.findRefereesAvailableForQueue).toHaveBeenLastCalledWith(2);
      expect(component.lockCount()).toBe(0);
      expect(component.savedAt()).toBeNull();
      expect(component.drawerMatchId()).toBeNull();
      expect(component.drawerMatch()).toBeNull();
      expect(component.drawerBreakdown()).toBeNull();
    });

    it('blocks the PDF export while the new queue loads and after a regenerate until it is saved', () => {
      expect(component.canExport()).toBe(true);

      const storedCast = new Subject<Match[]>();
      stafferService.getStoredCast.mockReturnValue(storedCast);
      component.incQueue();
      expect(component.canExport()).toBe(false);

      // The stored cast of the new queue agrees with the database, so it may be exported.
      storedCast.next(matches);
      storedCast.complete();
      expect(component.canExport()).toBe(true);

      component.generate();
      expect(component.canExport()).toBe(false);

      component.save();
      expect(component.canExport()).toBe(true);
    });

    it('keeps the cast when a step is a no-op — queue 1 cannot go lower', () => {
      component.toggleLock(component.matches()![0]);

      component.decQueue();

      expect(component.queue()).toBe(1);
      expect(component.matches()).toEqual(matches);
      expect(component.referees()).toEqual(referees);
      expect(component.lockCount()).toBe(1);
      expect(component.savedAt()).not.toBeNull();
    });

    it('falls back to the empty state so no stale row shows under the new queue heading', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelectorAll('tr.cast-row').length).toBe(3);

      component.incQueue();
      fixture.detectChanges();

      expect(el.querySelector('.empty-state')).not.toBeNull();
      expect(el.querySelector('.cast-panel')).toBeNull();
      expect(el.querySelector('.panel--kpi-strip')).toBeNull();
    });
  });

  describe('generate', () => {
    it('staffs the selected queue and populates matches, referees and standings lookups', () => {
      component.incQueue();
      component.generate();

      expect(stafferService.staffReferees).toHaveBeenCalledWith(2, []);
      expect(refereeService.findRefereesAvailableForQueue).toHaveBeenCalledWith(2);
      expect(component.matches()).toEqual(matches);
      expect(component.referees()).toEqual(referees);
      expect(component.totalTeams()).toBe(8);
      expect(component.getTeam(1)?.name).toBe('Alfa');
      expect(component.getTeam(999)).toBeUndefined();
    });

    it('keeps loading true until the forkJoin completes and clears it on success', () => {
      const staffSubject = new Subject<Match[]>();
      stafferService.staffReferees.mockReturnValue(staffSubject);

      component.generate();
      expect(component.loading()).toBe(true);

      staffSubject.next(matches);
      staffSubject.complete();
      expect(component.loading()).toBe(false);
    });

    it('clears loading and leaves the cast on screen untouched on error', () => {
      stafferService.getStoredCast.mockReturnValue(of(matches));
      component.loadStoredCast();
      stafferService.staffReferees.mockReturnValue(throwError(() => new Error('boom')));

      component.generate();

      expect(component.loading()).toBe(false);
      expect(component.matches()).toEqual(matches);
      // The stored cast is still on screen, but a failed request leaves the screen in an
      // unknown state, so it stops claiming to agree with the database until it reloads.
      expect(component.persisted()).toBe(false);
      expect(component.loadFailed()).toBe(true);
    });

    it('marks the cast as a draft until it is saved', () => {
      component.generate();

      expect(component.persisted()).toBe(false);
      expect(component.canExport()).toBe(false);

      component.save();

      expect(component.persisted()).toBe(true);
    });

    it('resets the saved-at marker so a stale "Saved" note never shows for a new cast', () => {
      component.generate();
      component.save();
      expect(component.savedAt()).not.toBeNull();

      component.generate();
      expect(component.savedAt()).toBeNull();
    });

    it('discards a staffing response for a queue the user has already stepped away from', () => {
      const staffSubject = new Subject<Match[]>();
      stafferService.staffReferees.mockReturnValue(staffSubject);

      component.generate();
      component.incQueue();
      staffSubject.next(matches);
      staffSubject.complete();

      // Still queue 2's stored cast (empty in this fixture), not the late draft for queue 1.
      expect(component.matches()).toEqual([]);
      expect(component.loading()).toBe(false);
    });

    it('re-enables the Generate button when the queue changes mid-request', () => {
      stafferService.staffReferees.mockReturnValue(new Subject<Match[]>());

      component.generate();
      expect(component.loading()).toBe(true);

      component.incQueue();
      expect(component.loading()).toBe(false);
    });

    it('does not resurrect the cast when the user steps back to the queue it was generated for', () => {
      const staffSubject = new Subject<Match[]>();
      stafferService.staffReferees.mockReturnValue(staffSubject);

      component.generate();
      component.incQueue();
      component.decQueue();
      staffSubject.next(matches);
      staffSubject.complete();

      expect(component.queue()).toBe(1);
      // Queue 1's stored cast was reloaded on the way back (empty here); the draft is not.
      expect(component.matches()).toEqual([]);
    });

    it('supersedes an earlier request so it cannot land last and win', () => {
      const first = new Subject<Match[]>();
      const second = new Subject<Match[]>();
      stafferService.staffReferees.mockReturnValueOnce(first).mockReturnValueOnce(second);

      const onScreen = component.matches();
      component.generate();
      component.generate();

      first.next([matches[0]]);
      first.complete();
      expect(component.matches()).toEqual(onScreen);

      second.next(matches);
      second.complete();
      expect(component.matches()).toEqual(matches);
    });

    it('sends the locked pairs so a regenerate preserves them server-side', () => {
      component.generate();
      component.toggleLock(component.matches()![0]);

      component.generate();

      expect(stafferService.staffReferees).toHaveBeenCalledWith(1, [{matchId: 11, refereeId: 100}]);
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
      expect(component.isLocked(assigned)).toBe(true);
      expect(component.lockCount()).toBe(1);

      component.toggleLock(assigned);
      expect(component.isLocked(assigned)).toBe(false);
      expect(component.lockCount()).toBe(0);
    });

    it('ignores locking a match without an assigned referee', () => {
      const unassigned = component.matches()!.find(m => m.id === 12)!;

      component.toggleLock(unassigned);

      expect(component.isLocked(unassigned)).toBe(false);
      expect(component.lockCount()).toBe(0);
    });

    it('clears all locks at once', () => {
      component.toggleLock(component.matches()![0]);
      component.toggleLock(component.matches()![2]);
      expect(component.lockCount()).toBe(2);

      component.clearLocks();
      expect(component.lockCount()).toBe(0);
    });

    it('clears locks on queue change — they reference matches of the previous queue', () => {
      component.toggleLock(component.matches()![0]);
      expect(component.lockCount()).toBe(1);

      component.incQueue();
      expect(component.lockCount()).toBe(0);

      component.generate();
      component.toggleLock(component.matches()![0]);
      expect(component.lockCount()).toBe(1);

      component.decQueue();
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
      expect(component.isLocked(target)).toBe(true);
      expect(component.locks().get(12)).toBe(102);
    });

    it('invalidates the saved-at marker and the stored-cast flag', () => {
      component.save();
      expect(component.savedAt()).not.toBeNull();
      expect(component.persisted()).toBe(true);

      component.swap(component.matches()![0], 103);
      expect(component.savedAt()).toBeNull();
      expect(component.persisted()).toBe(false);
      expect(component.canExport()).toBe(false);
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
      matchService.getDifficultyBreakdown.mockReturnValueOnce(first).mockReturnValueOnce(second);
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

      expect(byId.get(100)!.isAssigned).toBe(true);
      expect(byId.get(100)!.isUsedElsewhere).toBe(false);
      expect(byId.get(101)!.isUsedElsewhere).toBe(true);
      expect(byId.get(102)!.isUsedElsewhere).toBe(false);
      expect(byId.get(103)!.isUsedElsewhere).toBe(false);
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
      expect(component.flags(makeMatch(23, {homeTeamId: 3, awayTeamId: 4})).isTop).toBe(false);
      expect(component.flags(makeMatch(24, {homeTeamId: 5, awayTeamId: 8})).isBot).toBe(false);
    });

    it('treats the zone boundaries as inclusive', () => {
      // Places 1 vs 3 are both <= 3; places 6 vs 8 are both > 8 - 3.
      expect(component.flags(makeMatch(25, {homeTeamId: 1, awayTeamId: 3})).isTop).toBe(true);
      expect(component.flags(makeMatch(26, {homeTeamId: 6, awayTeamId: 8})).isBot).toBe(true);
    });

    it('reports no flags for a mid-table cross-city pairing', () => {
      expect(component.hasNoFlags(makeMatch(27, {homeTeamId: 4, awayTeamId: 5}))).toBe(true);
    });

    it('degrades to no flags for teams missing from the standings', () => {
      expect(component.hasNoFlags(makeMatch(28, {homeTeamId: 998, awayTeamId: 999}))).toBe(true);
    });

    it('follows the configured edge size instead of a hardcoded 3', () => {
      edgeTeams.set(2);

      // Places 1 vs 3 stop being a top pairing once the edge shrinks to 2...
      expect(component.flags(makeMatch(29, {homeTeamId: 1, awayTeamId: 3})).isTop).toBe(false);
      expect(component.flags(makeMatch(30, {homeTeamId: 1, awayTeamId: 2})).isTop).toBe(true);
      // ...and places 6 vs 8 leave the relegation zone (now places > 6).
      expect(component.flags(makeMatch(31, {homeTeamId: 6, awayTeamId: 8})).isBot).toBe(false);
      expect(component.flags(makeMatch(32, {homeTeamId: 7, awayTeamId: 8})).isBot).toBe(true);
    });
  });

  describe('save', () => {
    it('sends the current cast and stamps the save time', () => {
      component.generate();

      component.save();

      expect(matchService.updateList).toHaveBeenCalledWith(component.matches()!);
      expect(component.savedAt()).toBeInstanceOf(Date);
    });

    it('does nothing while the queue has no cast to save', () => {
      component.save();

      expect(matchService.updateList).not.toHaveBeenCalled();
      expect(component.savedAt()).toBeNull();
    });
  });

  describe('exportPdf', () => {
    // The sheet renders persisted assignments, so every export starts from an accepted
    // cast: generate, then save.
    function generateAndSave(): void {
      component.generate();
      component.save();
    }

    beforeEach(() => {
      matchService.downloadAssignmentsPdf.mockReturnValue(of(new Blob(['%PDF-'], {type: 'application/pdf'})));
    });

    it('is blocked until a generated cast has been saved', () => {
      component.generate();
      expect(component.canExport()).toBe(false);

      component.save();
      expect(component.canExport()).toBe(true);
    });

    it('allows exporting a played queue, whose cast is empty but whose sheet is not', () => {
      // getStoredCast only returns matches the staffer may still re-decide, so a played
      // queue answers with an empty cast — the assignment sheet still covers the queue.
      stafferService.getStoredCast.mockReturnValue(of([]));

      component.loadStoredCast();
      component.exportPdf();

      expect(component.persisted()).toBe(true);
      expect(component.canExport()).toBe(true);
      expect(matchService.downloadAssignmentsPdf).toHaveBeenCalledWith(1);
    });

    it('is blocked while a cast request is in flight', () => {
      const slow = new Subject<Match[]>();
      stafferService.getStoredCast.mockReturnValue(slow);

      component.loadStoredCast();

      expect(component.canExport()).toBe(false);

      slow.next(matches);
      slow.complete();
      expect(component.canExport()).toBe(true);
    });

    it('blocks again once the cast is regenerated', () => {
      generateAndSave();

      component.generate();

      expect(component.canExport()).toBe(false);
    });

    it('does nothing while an unsaved draft is on screen', () => {
      component.generate();

      component.exportPdf();

      expect(matchService.downloadAssignmentsPdf).not.toHaveBeenCalled();
      expect(saveFile).not.toHaveBeenCalled();
      expect(component.exporting()).toBe(false);
    });

    it('saves the PDF for the selected queue under a queue-stamped name', () => {
      component.incQueue();
      generateAndSave();

      component.exportPdf();

      expect(matchService.downloadAssignmentsPdf).toHaveBeenCalledWith(2);
      expect(saveFile).toHaveBeenCalledWith(expect.any(Blob), 'referee-assignments-queue-2.pdf');
      expect(component.exporting()).toBe(false);
    });

    it('stamps the file with the queue the sheet was requested for, not the one on screen', () => {
      generateAndSave();
      const pdfSubject = new Subject<Blob>();
      matchService.downloadAssignmentsPdf.mockReturnValue(pdfSubject);

      component.exportPdf();
      component.incQueue();
      pdfSubject.next(new Blob(['%PDF-'], {type: 'application/pdf'}));

      expect(matchService.downloadAssignmentsPdf).toHaveBeenCalledWith(1);
      expect(saveFile).toHaveBeenCalledWith(expect.any(Blob), 'referee-assignments-queue-1.pdf');
    });

    it('keeps exporting true until the download completes', () => {
      generateAndSave();
      const pdfSubject = new Subject<Blob>();
      matchService.downloadAssignmentsPdf.mockReturnValue(pdfSubject);

      component.exportPdf();
      expect(component.exporting()).toBe(true);

      pdfSubject.next(new Blob());
      expect(component.exporting()).toBe(false);
    });

    it('clears the exporting flag on error', () => {
      generateAndSave();
      matchService.downloadAssignmentsPdf.mockReturnValue(throwError(() => new Error('empty queue')));

      component.exportPdf();

      expect(saveFile).not.toHaveBeenCalled();
      expect(component.exporting()).toBe(false);
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

    describe('match without a date', () => {
      const dateless = makeMatch(14, {homeTeamId: 4, awayTeamId: 5, date: null, hardnessLvl: 40});

      beforeEach(() => {
        stafferService.staffReferees.mockReturnValue(of([...matches, dateless]));
        component.generate();
        fixture.detectChanges();
      });

      function rowOf(el: HTMLElement, matchId: number): HTMLElement {
        const index = component.sortedMatches().findIndex(m => m.id === matchId);
        return el.querySelectorAll<HTMLElement>('tr.cast-row')[index];
      }

      it('is listed with a no-date chip and says why it was not auto-staffed', () => {
        const row = rowOf(fixture.nativeElement, 14);
        expect(row.querySelector('.flag-cell')!.textContent).toContain('no date');
        expect(row.querySelector('.referee-cell')!.textContent).toContain('not auto-staffed');
      });

      it('keeps the plain "unassigned" label for a dated match without a referee', () => {
        const row = rowOf(fixture.nativeElement, 12);
        expect(row.querySelector('.referee-cell')!.textContent).toContain('unassigned');
        expect(row.querySelector('.referee-cell')!.textContent).not.toContain('not auto-staffed');
      });

      it('explains in the drawer that a referee can still be picked by hand', () => {
        component.openDrawer(dateless);
        fixture.detectChanges();
        expect(document.querySelector('.no-date-note')?.textContent).toContain('pick a referee');
      });

      it('does not show the drawer note for a dated match', () => {
        component.openDrawer(matches[1]);
        fixture.detectChanges();
        expect(document.querySelector('.no-date-note')).toBeNull();
      });

      it('can be staffed by hand through the drawer', () => {
        component.swap(dateless, 103);
        expect(component.matches()!.find(m => m.id === 14)!.refereeId).toBe(103);
        expect(component.isLocked(dateless)).toBe(true);
      });
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

import type {MockedObject} from 'vitest';
import {EnvironmentProviders, Provider, ɵprovideZonelessChangeDetectionInternal as provideZonelessChangeDetectionInternal} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NgForm} from '@angular/forms';
import {of, Subject} from 'rxjs';
import {MatchFormComponent} from './match-form.component';
import {MatchService} from '../../service/match.service';
import {TeamService} from '../../service/team.service';
import {RefereeService} from '../../service/referee.service';
import {GradeService} from '../../service/grade.service';
import {Match} from '../../model/match';
import {Team} from '../../model/team';
import {Referee} from '../../model/referee';
import {Grade} from '../../model/grade';
import {createMock} from '../../testing/mock';

describe('MatchFormComponent', () => {
  let matchService: MockedObject<MatchService>;
  let teamService: MockedObject<TeamService>;
  let refereeService: MockedObject<RefereeService>;
  let gradeService: MockedObject<GradeService>;

  const teams: Team[] = [
    {id: 1, name: 'Alfa', city: 'Krakow'},
    {id: 2, name: 'Beta', city: 'Gdansk'},
    {id: 3, name: 'Gamma', city: 'Lodz'}
  ];

  const referees: Referee[] = [
    {id: 100, firstName: 'Jan', lastName: 'Kowalski', email: 'jan@example.com', experience: 10},
    {id: 101, firstName: 'Anna', lastName: 'Nowak', email: 'anna@example.com', experience: 4}
  ];

  function makeMatch(overrides: Partial<Match> = {}): Match {
    return {
      id: 7,
      queue: 3,
      homeTeamId: 1,
      awayTeamId: 2,
      date: '2026-03-01T12:00:00',
      refereeId: 100,
      homeScore: 2,
      awayScore: 1,
      ...overrides
    };
  }

  const validForm = {valid: true} as NgForm;
  const invalidForm = {valid: false} as NgForm;

  async function configureTestBed(extraProviders: (Provider | EnvironmentProviders)[] = []): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [MatchFormComponent],
      providers: [
        {provide: MatchService, useValue: matchService},
        {provide: TeamService, useValue: teamService},
        {provide: RefereeService, useValue: refereeService},
        {provide: GradeService, useValue: gradeService},
        ...extraProviders
      ]
    }).compileComponents();
  }

  beforeEach(async () => {
    matchService = createMock<MatchService>(['save', 'update']);
    teamService = createMock<TeamService>(['findAll']);
    refereeService = createMock<RefereeService>(['findAll']);
    gradeService = createMock<GradeService>(['findById', 'save', 'update', 'delete']);

    teamService.findAll.mockReturnValue(of(teams));
    refereeService.findAll.mockReturnValue(of(referees));

    await configureTestBed();
  });

  function createComponent(match: Match | null): ComponentFixture<MatchFormComponent> {
    const fixture = TestBed.createComponent(MatchFormComponent);
    fixture.componentInstance.match = match;
    fixture.detectChanges();
    return fixture;
  }

  describe('initialization', () => {
    it('loads teams and referees for the selects in add mode', () => {
      const component = createComponent(null).componentInstance;

      expect(component.teams()).toEqual(teams);
      expect(component.referees()).toEqual(referees);
      expect(component.editMode).toBe(false);
      expect(component.model).toEqual({});
      expect(component.existingGrade()).toBeNull();
      expect(gradeService.findById).not.toHaveBeenCalled();
    });

    it('copies the editable fields of the edited match, leaving identity out of the draft', () => {
      gradeService.findById.mockReturnValue(of({id: 5, value: 8.1}));
      const match = makeMatch({gradeId: 5});
      const component = createComponent(match).componentInstance;

      expect(component.editMode).toBe(true);
      expect(component.model).toEqual({
        queue: 3,
        date: match.date,
        homeTeamId: 1,
        awayTeamId: 2,
        refereeId: 100,
        homeScore: 2,
        awayScore: 1
      });
      // The draft describes a create payload: the backend owns id and gradeId.
      expect(component.model).not.toHaveProperty('id');
      expect(component.model).not.toHaveProperty('gradeId');

      component.model.queue = 99;
      expect(match.queue).toBe(3);
    });

    it('fetches the existing grade when the match references one and splits it from the draft', () => {
      const grade: Grade = {id: 5, value: 8.1};
      gradeService.findById.mockReturnValue(of(grade));

      const component = createComponent(makeMatch({gradeId: 5})).componentInstance;

      expect(gradeService.findById).toHaveBeenCalledWith(5);
      // The id stays on existingGrade; the draft holds only what the inputs bind to.
      expect(component.existingGrade()).toEqual(grade);
      expect(component.grade()).toEqual({value: 8.1, secondValue: undefined});
      expect(component.grade()).not.toHaveProperty('id');
    });

    it('fetches the stored grade even when its id is 0', () => {
      // `gradeId` is guarded with an explicit null check, so a falsy-but-real id
      // still resolves. Not reachable with Hibernate-generated ids today, but it is
      // what separates the guard from a truthiness test.
      const grade: Grade = {id: 0, value: 8.1};
      gradeService.findById.mockReturnValue(of(grade));

      const component = createComponent(makeMatch({gradeId: 0})).componentInstance;

      expect(gradeService.findById).toHaveBeenCalledWith(0);
      expect(component.existingGrade()).toEqual(grade);
    });

    it('shows the mode in the drawer title', () => {
      const addFixture = createComponent(null);
      expect((addFixture.nativeElement as HTMLElement).querySelector('.drawer__title')?.textContent)
        .toContain('Add match');
      addFixture.destroy();

      const editFixture = createComponent(makeMatch());
      expect((editFixture.nativeElement as HTMLElement).querySelector('.drawer__title')?.textContent)
        .toContain('Edit match');
    });
  });

  // RS-116: teams, referees and the stored grade arrive from HTTP after the drawer is
  // already rendered, and the drawer used to show them only once something else happened
  // to repaint the view.
  //
  // These run zoneless on purpose. TestBed is zone-based by default, which repaints a
  // CheckAlways component on any async boundary and so hides the defect entirely — under
  // zone-based change detection this block passes against the plain-field version too.
  // Zoneless is both the mode the bug appeared in and the one the component must survive
  // on its own, independently of what app.config.ts provides. The internal provider is the
  // one `bootstrapApplication` prepends; the public `provideZonelessChangeDetection()` has
  // the same providers but warns (NG0914) whenever zone.js is loaded, which every run of
  // this suite is.
  describe('selects filled from asynchronous responses', () => {
    let teams$: Subject<Team[]>;
    let referees$: Subject<Referee[]>;
    let grade$: Subject<Grade>;

    beforeEach(async () => {
      teams$ = new Subject<Team[]>();
      referees$ = new Subject<Referee[]>();
      grade$ = new Subject<Grade>();
      teamService.findAll.mockReturnValue(teams$);
      refereeService.findAll.mockReturnValue(referees$);

      TestBed.resetTestingModule();
      await configureTestBed([provideZonelessChangeDetectionInternal()]);
    });

    function selectOf(fixture: ComponentFixture<MatchFormComponent>, id: string): HTMLSelectElement {
      return (fixture.nativeElement as HTMLElement).querySelector<HTMLSelectElement>(id)!;
    }

    /** Option labels a user can actually pick — the hidden placeholders are not choices. */
    function offeredOptions(select: HTMLSelectElement): string[] {
      return Array.from(select.options).filter(option => !option.hidden).map(option => option.textContent!.trim());
    }

    function selectedLabel(select: HTMLSelectElement): string | null {
      return select.selectedIndex < 0 ? null : select.options[select.selectedIndex].textContent!.trim();
    }

    /** Renders the drawer, then lets the responses land the way the network would. */
    async function createAndDeliverResponses(match: Match | null): Promise<ComponentFixture<MatchFormComponent>> {
      const fixture = createComponent(match);
      fixture.autoDetectChanges();

      // Nothing from the responses is rendered yet, so whatever the assertions below
      // find got there because the responses themselves repainted the drawer.
      expect(offeredOptions(selectOf(fixture, '#homeTeam'))).toEqual([]);

      teams$.next(teams);
      referees$.next(referees);
      // No manual detectChanges() here on purpose.
      await fixture.whenStable();
      return fixture;
    }

    it('shows the edited match selections once the responses land', async () => {
      const fixture = await createAndDeliverResponses(makeMatch());

      expect(selectedLabel(selectOf(fixture, '#homeTeam'))).toBe('Alfa · Krakow');
      expect(selectedLabel(selectOf(fixture, '#awayTeam'))).toBe('Beta · Gdansk');
      expect(selectedLabel(selectOf(fixture, '#referee'))).toBe('Jan Kowalski');
    });

    it('offers every team but the opponent, and every referee, without any interaction', async () => {
      const fixture = await createAndDeliverResponses(makeMatch());

      // The opposite side is filtered out by the excludeValue pipe.
      expect(offeredOptions(selectOf(fixture, '#homeTeam'))).toEqual(['Alfa · Krakow', 'Gamma · Lodz']);
      expect(offeredOptions(selectOf(fixture, '#awayTeam'))).toEqual(['Beta · Gdansk', 'Gamma · Lodz']);
      expect(offeredOptions(selectOf(fixture, '#referee')))
        .toEqual(['Unassigned — run staffer to fill', 'Jan Kowalski', 'Anna Nowak']);
    });

    it('fills the add-mode selects too, with nothing preselected', async () => {
      const fixture = await createAndDeliverResponses(null);

      expect(offeredOptions(selectOf(fixture, '#homeTeam')))
        .toEqual(['Alfa · Krakow', 'Beta · Gdansk', 'Gamma · Lodz']);
      expect(offeredOptions(selectOf(fixture, '#referee')))
        .toEqual(['Unassigned — run staffer to fill', 'Jan Kowalski', 'Anna Nowak']);
      // Nothing real is preselected — the hidden placeholder still holds the selection.
      expect(selectedLabel(selectOf(fixture, '#homeTeam'))).toBe('Home');
    });

    it('renders the stored grade once its response lands', async () => {
      gradeService.findById.mockReturnValue(grade$);
      const fixture = await createAndDeliverResponses(makeMatch({gradeId: 5}));

      grade$.next({id: 5, value: 7.9, secondValue: 8.3});
      await fixture.whenStable();

      const input = (id: string) => (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>(id)!;
      expect(input('#grade').value).toBe('7.9');
      expect(input('#secondGrade').value).toBe('8.3');
    });
  });

  describe('submit in add mode', () => {
    it('does nothing while the form is invalid', () => {
      const component = createComponent(null).componentInstance;

      component.onSubmit(invalidForm);

      expect(matchService.save).not.toHaveBeenCalled();
      expect(matchService.update).not.toHaveBeenCalled();
    });

    it('saves the match and emits it when no grade was entered', () => {
      const component = createComponent(null).componentInstance;
      const saved = makeMatch({id: 42});
      matchService.save.mockReturnValue(of(saved));
      const emitted: Match[] = [];
      component.saved.subscribe(m => emitted.push(m));

      component.model = {queue: 3, homeTeamId: 1, awayTeamId: 2};
      component.onSubmit(validForm);

      expect(matchService.save).toHaveBeenCalledWith({queue: 3, homeTeamId: 1, awayTeamId: 2, date: null});
      expect(matchService.save.mock.calls[0][0]).not.toHaveProperty('id');
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(emitted).toEqual([saved]);
    });

    it('sends an explicit null date when the date input was left empty', () => {
      const component = createComponent(null).componentInstance;
      matchService.save.mockReturnValue(of(makeMatch({id: 42, date: null})));

      // A cleared datetime-local input binds an empty string, not null.
      component.model = {queue: 3, homeTeamId: 1, awayTeamId: 2, date: ''} as Match;
      component.onSubmit(validForm);

      expect(matchService.save).toHaveBeenCalledWith(expect.objectContaining({date: null}));
    });

    it('keeps a scheduled date as entered', () => {
      const component = createComponent(null).componentInstance;
      matchService.save.mockReturnValue(of(makeMatch({id: 42})));
      const date = '2026-05-10T18:00';

      component.model = {queue: 3, homeTeamId: 1, awayTeamId: 2, date} as Match;
      component.onSubmit(validForm);

      expect(matchService.save).toHaveBeenCalledWith(expect.objectContaining({date}));
    });

    it('does not require a date — fixtures are planned before kick-off is scheduled', () => {
      const input = (createComponent(null).nativeElement as HTMLElement)
        .querySelector<HTMLInputElement>('input#date');
      expect(input).not.toBeNull();
      expect(input!.required).toBe(false);
    });

    it('posts every optional draft field alongside the mandatory ones', () => {
      const component = createComponent(null).componentInstance;
      matchService.save.mockReturnValue(of(makeMatch({id: 42})));

      // A full draft: the payload must carry the optional fields too, not just the three
      // the create contract marks mandatory.
      const draft = {
        queue: 3,
        homeTeamId: 1,
        awayTeamId: 2,
        date: '2026-03-01T12:00',
        refereeId: 100,
        homeScore: 2,
        awayScore: 1
      };
      component.model = {...draft};
      component.onSubmit(validForm);

      expect(matchService.save).toHaveBeenCalledWith(draft);
    });

    it('posts nothing when a mandatory field is missing, even if the form claims to be valid', () => {
      const component = createComponent(null).componentInstance;

      // Only the queue was filled: a create payload needs both teams too.
      component.model = {queue: 3};
      component.onSubmit(validForm);

      expect(matchService.save).not.toHaveBeenCalled();
      expect(gradeService.save).not.toHaveBeenCalled();
    });

    it('saves a split grade with both components', () => {
      const component = createComponent(null).componentInstance;
      const saved = makeMatch({id: 42});
      matchService.save.mockReturnValue(of(saved));
      gradeService.save.mockReturnValue(of({id: 6, value: 7.9, secondValue: 8.3}));

      component.model = {queue: 3, homeTeamId: 1, awayTeamId: 2};
      component.grade().value = 7.9;
      component.grade().secondValue = 8.3;
      component.onSubmit(validForm);

      expect(gradeService.save).toHaveBeenCalledWith(saved, {value: 7.9, secondValue: 8.3});
      expect(gradeService.save.mock.calls[0][1]).not.toHaveProperty('id');
      expect(component.grade().secondValue).toBe(8.3);
    });

    it('creates no grade from a second component alone', () => {
      const component = createComponent(null).componentInstance;
      const saved = makeMatch({id: 42});
      matchService.save.mockReturnValue(of(saved));
      const emitted: Match[] = [];
      component.saved.subscribe(m => emitted.push(m));

      component.model = {queue: 3, homeTeamId: 1, awayTeamId: 2};
      component.grade().secondValue = 8.3;
      component.onSubmit(validForm);

      expect(gradeService.save).not.toHaveBeenCalled();
      expect(emitted).toEqual([saved]);
    });

    it('saves a grade of 0 — zero is an entered value, not an empty input', () => {
      const component = createComponent(null).componentInstance;
      const saved = makeMatch({id: 42});
      matchService.save.mockReturnValue(of(saved));
      gradeService.save.mockReturnValue(of({id: 6, value: 0}));
      const emitted: Match[] = [];
      component.saved.subscribe(m => emitted.push(m));

      component.model = {queue: 3, homeTeamId: 1, awayTeamId: 2};
      component.grade().value = 0;
      component.onSubmit(validForm);

      expect(gradeService.save).toHaveBeenCalledWith(saved, {value: 0, secondValue: undefined});
      expect(emitted).toEqual([saved]);
    });

    it('saves an entered grade against the newly created match before emitting', () => {
      const component = createComponent(null).componentInstance;
      const saved = makeMatch({id: 42});
      matchService.save.mockReturnValue(of(saved));
      const gradeSave = new Subject<Grade>();
      gradeService.save.mockReturnValue(gradeSave);
      const emitted: Match[] = [];
      component.saved.subscribe(m => emitted.push(m));

      component.model = {queue: 3, homeTeamId: 1, awayTeamId: 2};
      component.grade().value = 8.4;
      component.onSubmit(validForm);

      // The grade must be attached to the match id returned by the backend,
      // and `saved` must not fire until the grade round-trip finishes.
      expect(gradeService.save).toHaveBeenCalledWith(saved, {value: 8.4, secondValue: undefined});
      expect(emitted).toEqual([]);

      gradeSave.next({id: 6, value: 8.4});
      expect(emitted).toEqual([saved]);
    });
  });

  describe('submit in edit mode', () => {
    let component: MatchFormComponent;
    let edited: Match;
    let updated: Match;
    let emitted: Match[];

    beforeEach(() => {
      updated = makeMatch({queue: 4});
      matchService.update.mockReturnValue(of(updated));
      emitted = [];
    });

    function createInEditMode(gradeId?: number, storedGrade?: Grade): void {
      if (storedGrade) gradeService.findById.mockReturnValue(of(storedGrade));
      edited = makeMatch({gradeId});
      component = createComponent(edited).componentInstance;
      component.saved.subscribe(m => emitted.push(m));
    }

    it('updates the match and emits it when the grade was never touched', () => {
      createInEditMode();

      // Touch an optional field so the assert cannot pass on the untouched copy alone.
      component.model.awayScore = 3;
      component.onSubmit(validForm);

      // The draft is spread back over the edited row, so id and gradeId survive.
      expect(matchService.update).toHaveBeenCalledWith({...edited, awayScore: 3});
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(gradeService.update).not.toHaveBeenCalled();
      expect(gradeService.delete).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('updates an existing grade when its value was changed', () => {
      createInEditMode(5, {id: 5, value: 7.5});
      gradeService.update.mockReturnValue(of({id: 5, value: 8.0}));

      component.grade().value = 8.0;
      component.onSubmit(validForm);

      // The stored id comes from existingGrade, the value from the draft.
      expect(gradeService.update).toHaveBeenCalledWith({id: 5, value: 8.0, secondValue: undefined});
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(gradeService.delete).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('creates the grade when one was entered for a match without one', () => {
      createInEditMode();
      gradeService.save.mockReturnValue(of({id: 6, value: 8.2}));

      component.grade().value = 8.2;
      component.onSubmit(validForm);

      expect(gradeService.save).toHaveBeenCalledWith(updated, {value: 8.2, secondValue: undefined});
      expect(gradeService.update).not.toHaveBeenCalled();
      expect(gradeService.delete).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('updates an existing grade when its value is changed to 0 instead of deleting it', () => {
      createInEditMode(5, {id: 5, value: 7.5});
      gradeService.update.mockReturnValue(of({id: 5, value: 0}));

      component.grade().value = 0;
      component.onSubmit(validForm);

      expect(gradeService.update).toHaveBeenCalledWith(expect.objectContaining({id: 5, value: 0}));
      expect(gradeService.delete).not.toHaveBeenCalled();
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('deletes the grade when its value was cleared', () => {
      createInEditMode(5, {id: 5, value: 7.5});
      const gradeDelete = new Subject<void>();
      gradeService.delete.mockReturnValue(gradeDelete);

      component.grade().value = undefined;
      component.onSubmit(validForm);

      expect(gradeService.delete).toHaveBeenCalledWith({id: 5, value: 7.5});
      expect(gradeService.update).not.toHaveBeenCalled();
      expect(gradeService.save).not.toHaveBeenCalled();
      // Emission waits for the delete to complete.
      expect(emitted).toEqual([]);
      gradeDelete.next();
      expect(emitted).toEqual([updated]);
    });

    it('deletes the grade when the number input hands back null rather than undefined', () => {
      createInEditMode(5, {id: 5, value: 7.5});
      gradeService.delete.mockReturnValue(of(undefined as unknown as void));

      component.grade().value = null as unknown as number;
      component.onSubmit(validForm);

      expect(gradeService.delete).toHaveBeenCalledWith(component.existingGrade());
      expect(gradeService.update).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('treats a stored grade with id 0 as stored, so clearing it deletes instead of creating', () => {
      createInEditMode(0, {id: 0, value: 7.5});
      gradeService.delete.mockReturnValue(of(undefined as unknown as void));

      component.grade().value = null as unknown as number;
      component.onSubmit(validForm);

      expect(gradeService.delete).toHaveBeenCalledWith(component.existingGrade());
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('deletes a stored grade left with only a second component — it never reaches the backend alone', () => {
      createInEditMode(5, {id: 5, value: 7.5, secondValue: 8.3});
      gradeService.delete.mockReturnValue(of(undefined as unknown as void));

      component.grade().value = undefined as unknown as number;
      component.onSubmit(validForm);

      expect(gradeService.delete).toHaveBeenCalledWith(component.existingGrade());
      expect(gradeService.update).not.toHaveBeenCalled();
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('does not touch any service while the form is invalid', () => {
      createInEditMode();

      component.onSubmit(invalidForm);

      expect(matchService.update).not.toHaveBeenCalled();
      expect(emitted).toEqual([]);
    });
  });

  describe('grade still loading', () => {
    // RS-126: the stored grade is fetched asynchronously. A save that beat the response
    // used to read the empty `grade.id` as "no grade stored" and POST a second grade for
    // the same match; the match has only one grade row, so the duplicate is never valid.
    let findById: Subject<Grade>;
    let component: MatchFormComponent;
    let fixture: ComponentFixture<MatchFormComponent>;
    let emitted: Match[];

    beforeEach(() => {
      findById = new Subject<Grade>();
      gradeService.findById.mockReturnValue(findById);
      matchService.update.mockReturnValue(of(makeMatch({queue: 4})));
      fixture = createComponent(makeMatch({gradeId: 5}));
      component = fixture.componentInstance;
      emitted = [];
      component.saved.subscribe(m => emitted.push(m));
    });

    function gradeInputs(): HTMLInputElement[] {
      const host = fixture.nativeElement as HTMLElement;
      return [
        host.querySelector<HTMLInputElement>('input#grade')!,
        host.querySelector<HTMLInputElement>('input#secondGrade')!
      ];
    }

    it('reports the grade as loading until the response arrives', () => {
      expect(component.gradeLoading()).toBe(true);
      expect(component.gradeEditable).toBe(false);
      expect(component.canSubmit).toBe(false);

      findById.next({id: 5, value: 7.5});

      expect(component.gradeLoading()).toBe(false);
      expect(component.gradeEditable).toBe(true);
      expect(component.canSubmit).toBe(true);
    });

    it('disables both grade inputs while loading, so no value can be typed and then lost', async () => {
      await fixture.whenStable();
      expect(gradeInputs().map(i => i.disabled)).toEqual([true, true]);

      findById.next({id: 5, value: 7.5});
      fixture.detectChanges();
      await fixture.whenStable();

      // The first input is editable again; the second one stays gated on the first value.
      expect(gradeInputs()[0].disabled).toBe(false);
      expect(component.existingGrade()).toEqual({id: 5, value: 7.5});
      expect(component.grade()).toEqual({value: 7.5, secondValue: undefined});
    });

    it('does not save anything while the grade is loading', () => {
      // The pre-fix failure mode: a grade value present with `grade.id` still undefined.
      component.grade().value = 8.2;
      component.onSubmit(validForm);

      // The duplicate POST is the bug; the match update is held back with it.
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(gradeService.update).not.toHaveBeenCalled();
      expect(gradeService.delete).not.toHaveBeenCalled();
      expect(matchService.update).not.toHaveBeenCalled();
      expect(emitted).toEqual([]);
    });

    it('updates the stored grade once it has loaded instead of creating a second one', () => {
      findById.next({id: 5, value: 7.5});
      gradeService.update.mockReturnValue(of({id: 5, value: 8.2}));

      component.grade().value = 8.2;
      component.onSubmit(validForm);

      expect(gradeService.update).toHaveBeenCalledWith(expect.objectContaining({id: 5, value: 8.2}));
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(emitted).toEqual([makeMatch({queue: 4})]);
    });
  });

  describe('grade load failure', () => {
    it('saves the match but leaves the unread grade untouched', () => {
      const findById = new Subject<Grade>();
      gradeService.findById.mockReturnValue(findById);
      const updated = makeMatch({queue: 4});
      matchService.update.mockReturnValue(of(updated));
      const component = createComponent(makeMatch({gradeId: 5})).componentInstance;
      const emitted: Match[] = [];
      component.saved.subscribe(m => emitted.push(m));

      findById.error(new Error('boom'));

      expect(component.gradeLoading()).toBe(false);
      expect(component.gradeLoadFailed()).toBe(true);
      // Submit is allowed again — only the grade section is off limits.
      expect(component.canSubmit).toBe(true);
      expect(component.gradeEditable).toBe(false);

      component.onSubmit(validForm);

      expect(matchService.update).toHaveBeenCalledWith(expect.objectContaining(component.model));
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(gradeService.update).not.toHaveBeenCalled();
      // Above all: the empty input must not be read as "grade cleared" and delete it.
      expect(gradeService.delete).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });
  });

  describe('split grade input', () => {
    it('drops the second component when the first one is cleared', () => {
      const component = createComponent(null).componentInstance;
      component.grade().value = 7.9;
      component.grade().secondValue = 8.3;

      component.onGradeValueChange(null);

      expect(component.grade().secondValue).toBeUndefined();
    });

    it('treats a first component of 0 as present, so the second input stays enabled', () => {
      const component = createComponent(null).componentInstance;
      component.grade().value = 0;
      component.grade().secondValue = 8.3;

      component.onGradeValueChange(0);

      expect(component.gradeValueMissing).toBe(false);
      expect(component.grade().secondValue).toBe(8.3);
    });

    it('keeps the second component while the first one has a value', () => {
      const component = createComponent(null).componentInstance;
      component.grade().value = 7.9;
      component.grade().secondValue = 8.3;

      component.onGradeValueChange(8.0);

      expect(component.grade().secondValue).toBe(8.3);
    });
  });
});

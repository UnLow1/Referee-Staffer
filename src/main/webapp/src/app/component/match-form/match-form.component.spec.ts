import type {MockedObject} from 'vitest';
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
    {id: 1, name: 'Alfa', city: 'Krakow', points: 40},
    {id: 2, name: 'Beta', city: 'Gdansk', points: 30},
    {id: 3, name: 'Gamma', city: 'Lodz', points: 20}
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
      date: new Date('2026-03-01T12:00:00'),
      refereeId: 100,
      gradeId: undefined,
      homeScore: 2,
      awayScore: 1,
      ...overrides
    } as Match;
  }

  const validForm = {valid: true} as NgForm;
  const invalidForm = {valid: false} as NgForm;

  beforeEach(async () => {
    matchService = createMock<MatchService>(['save', 'update']);
    teamService = createMock<TeamService>(['findAll']);
    refereeService = createMock<RefereeService>(['findAll']);
    gradeService = createMock<GradeService>(['findById', 'save', 'update', 'delete']);

    teamService.findAll.mockReturnValue(of(teams));
    refereeService.findAll.mockReturnValue(of(referees));

    await TestBed.configureTestingModule({
      imports: [MatchFormComponent],
      providers: [
        {provide: MatchService, useValue: matchService},
        {provide: TeamService, useValue: teamService},
        {provide: RefereeService, useValue: refereeService},
        {provide: GradeService, useValue: gradeService}
      ]
    }).compileComponents();
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
      expect(component.model).toEqual({} as Match);
      expect(gradeService.findById).not.toHaveBeenCalled();
    });

    it('copies the edited match into the form model so edits do not leak into the list', () => {
      const match = makeMatch();
      const component = createComponent(match).componentInstance;

      expect(component.editMode).toBe(true);
      expect(component.model).toEqual(match);
      expect(component.model).not.toBe(match);

      component.model.queue = 99;
      expect(match.queue).toBe(3);
    });

    it('fetches the existing grade when the match references one', () => {
      const grade: Grade = {id: 5, value: 8.1};
      gradeService.findById.mockReturnValue(of(grade));

      const component = createComponent(makeMatch({gradeId: 5})).componentInstance;

      expect(gradeService.findById).toHaveBeenCalledWith(5);
      expect(component.grade).toEqual(grade);
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

  // RS-116: teams and referees arrive from HTTP after the drawer is already rendered, and
  // the drawer used to show them only once something else happened to repaint the view.
  // These lock in the acceptance criteria — both selects carry their full option list and
  // the edited match's selection with no interaction in between.
  describe('selects filled from asynchronous responses', () => {
    let teams$: Subject<Team[]>;
    let referees$: Subject<Referee[]>;

    beforeEach(() => {
      teams$ = new Subject<Team[]>();
      referees$ = new Subject<Referee[]>();
      teamService.findAll.mockReturnValue(teams$);
      refereeService.findAll.mockReturnValue(referees$);
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

    /** Renders the drawer, then lets both responses land the way the network would. */
    async function createAndDeliverResponses(match: Match | null): Promise<ComponentFixture<MatchFormComponent>> {
      const fixture = createComponent(match);
      fixture.autoDetectChanges();

      teams$.next(teams);
      referees$.next(referees);
      // No manual detectChanges() here on purpose: the point of the regression is that
      // the responses themselves have to repaint the drawer.
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

    it('leaves the selects empty until the responses arrive', () => {
      const fixture = createComponent(null);

      expect(offeredOptions(selectOf(fixture, '#homeTeam'))).toEqual([]);
      expect(offeredOptions(selectOf(fixture, '#referee'))).toEqual(['Unassigned — run staffer to fill']);
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

      component.model = {queue: 3, homeTeamId: 1, awayTeamId: 2} as Match;
      component.onSubmit(validForm);

      expect(matchService.save).toHaveBeenCalledWith(component.model);
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(emitted).toEqual([saved]);
    });

    it('saves a split grade with both components', () => {
      const component = createComponent(null).componentInstance;
      const saved = makeMatch({id: 42});
      matchService.save.mockReturnValue(of(saved));
      gradeService.save.mockReturnValue(of({id: 6, value: 7.9, secondValue: 8.3}));

      component.grade.value = 7.9;
      component.grade.secondValue = 8.3;
      component.onSubmit(validForm);

      expect(gradeService.save).toHaveBeenCalledWith(saved, component.grade);
      expect(component.grade.secondValue).toBe(8.3);
    });

    it('saves an entered grade against the newly created match before emitting', () => {
      const component = createComponent(null).componentInstance;
      const saved = makeMatch({id: 42});
      matchService.save.mockReturnValue(of(saved));
      const gradeSave = new Subject<Grade>();
      gradeService.save.mockReturnValue(gradeSave);
      const emitted: Match[] = [];
      component.saved.subscribe(m => emitted.push(m));

      component.grade.value = 8.4;
      component.onSubmit(validForm);

      // The grade must be attached to the match id returned by the backend,
      // and `saved` must not fire until the grade round-trip finishes.
      expect(gradeService.save).toHaveBeenCalledWith(saved, component.grade);
      expect(emitted).toEqual([]);

      gradeSave.next(component.grade);
      expect(emitted).toEqual([saved]);
    });
  });

  describe('submit in edit mode', () => {
    let component: MatchFormComponent;
    let updated: Match;
    let emitted: Match[];

    beforeEach(() => {
      updated = makeMatch({queue: 4});
      matchService.update.mockReturnValue(of(updated));
      emitted = [];
    });

    function createInEditMode(gradeId?: number, storedGrade?: Grade): void {
      if (storedGrade) gradeService.findById.mockReturnValue(of(storedGrade));
      component = createComponent(makeMatch({gradeId})).componentInstance;
      component.saved.subscribe(m => emitted.push(m));
    }

    it('updates the match and emits it when the grade was never touched', () => {
      createInEditMode();

      component.onSubmit(validForm);

      expect(matchService.update).toHaveBeenCalledWith(component.model);
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(gradeService.update).not.toHaveBeenCalled();
      expect(gradeService.delete).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('updates an existing grade when its value was changed', () => {
      createInEditMode(5, {id: 5, value: 7.5});
      gradeService.update.mockReturnValue(of({id: 5, value: 8.0}));

      component.grade.value = 8.0;
      component.onSubmit(validForm);

      expect(gradeService.update).toHaveBeenCalledWith(component.grade);
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(gradeService.delete).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('creates the grade when one was entered for a match without one', () => {
      createInEditMode();
      gradeService.save.mockReturnValue(of({id: 6, value: 8.2}));

      component.grade.value = 8.2;
      component.onSubmit(validForm);

      expect(gradeService.save).toHaveBeenCalledWith(updated, component.grade);
      expect(gradeService.update).not.toHaveBeenCalled();
      expect(gradeService.delete).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('deletes the grade when its value was cleared', () => {
      createInEditMode(5, {id: 5, value: 7.5});
      const gradeDelete = new Subject<void>();
      gradeService.delete.mockReturnValue(gradeDelete);

      component.grade.value = undefined as unknown as number;
      component.onSubmit(validForm);

      expect(gradeService.delete).toHaveBeenCalledWith(component.grade);
      expect(gradeService.update).not.toHaveBeenCalled();
      expect(gradeService.save).not.toHaveBeenCalled();
      // Emission waits for the delete to complete.
      expect(emitted).toEqual([]);
      gradeDelete.next();
      expect(emitted).toEqual([updated]);
    });

    it('does not touch any service while the form is invalid', () => {
      createInEditMode();

      component.onSubmit(invalidForm);

      expect(matchService.update).not.toHaveBeenCalled();
      expect(emitted).toEqual([]);
    });
  });

  describe('split grade input', () => {
    it('drops the second component when the first one is cleared', () => {
      const component = createComponent(null).componentInstance;
      component.grade.value = 7.9;
      component.grade.secondValue = 8.3;

      component.onGradeValueChange(null);

      expect(component.grade.secondValue).toBeUndefined();
    });

    it('keeps the second component while the first one has a value', () => {
      const component = createComponent(null).componentInstance;
      component.grade.value = 7.9;
      component.grade.secondValue = 8.3;

      component.onGradeValueChange(8.0);

      expect(component.grade.secondValue).toBe(8.3);
    });
  });
});

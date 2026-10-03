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
    {id: 2, name: 'Beta', city: 'Gdansk', points: 30}
  ];

  const referees: Referee[] = [
    {id: 100, firstName: 'Jan', lastName: 'Kowalski', email: 'jan@example.com', experience: 10}
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

      expect(component.teams).toEqual(teams);
      expect(component.referees).toEqual(referees);
      expect(component.editMode).toBe(false);
      expect(component.model).toEqual({});
      expect(component.existingGrade).toBeNull();
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
      expect(component.existingGrade).toEqual(grade);
      expect(component.grade).toEqual({value: 8.1, secondValue: undefined});
      expect(component.grade).not.toHaveProperty('id');
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

      expect(matchService.save).toHaveBeenCalledWith({queue: 3, homeTeamId: 1, awayTeamId: 2});
      expect(matchService.save.mock.calls[0][0]).not.toHaveProperty('id');
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(emitted).toEqual([saved]);
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
      component.grade.value = 7.9;
      component.grade.secondValue = 8.3;
      component.onSubmit(validForm);

      expect(gradeService.save).toHaveBeenCalledWith(saved, {value: 7.9, secondValue: 8.3});
      expect(gradeService.save.mock.calls[0][1]).not.toHaveProperty('id');
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

      component.model = {queue: 3, homeTeamId: 1, awayTeamId: 2};
      component.grade.value = 8.4;
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

      component.onSubmit(validForm);

      // The draft is spread back over the edited row, so id and gradeId survive.
      expect(matchService.update).toHaveBeenCalledWith(edited);
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

      // The stored id comes from existingGrade, the value from the draft.
      expect(gradeService.update).toHaveBeenCalledWith({id: 5, value: 8.0, secondValue: undefined});
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(gradeService.delete).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('creates the grade when one was entered for a match without one', () => {
      createInEditMode();
      gradeService.save.mockReturnValue(of({id: 6, value: 8.2}));

      component.grade.value = 8.2;
      component.onSubmit(validForm);

      expect(gradeService.save).toHaveBeenCalledWith(updated, {value: 8.2, secondValue: undefined});
      expect(gradeService.update).not.toHaveBeenCalled();
      expect(gradeService.delete).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('deletes the grade when its value was cleared', () => {
      createInEditMode(5, {id: 5, value: 7.5});
      const gradeDelete = new Subject<void>();
      gradeService.delete.mockReturnValue(gradeDelete);

      component.grade.value = undefined;
      component.onSubmit(validForm);

      expect(gradeService.delete).toHaveBeenCalledWith({id: 5, value: 7.5});
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

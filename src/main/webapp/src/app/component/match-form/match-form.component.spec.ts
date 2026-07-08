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

describe('MatchFormComponent', () => {
  let matchService: jasmine.SpyObj<MatchService>;
  let teamService: jasmine.SpyObj<TeamService>;
  let refereeService: jasmine.SpyObj<RefereeService>;
  let gradeService: jasmine.SpyObj<GradeService>;

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
    matchService = jasmine.createSpyObj('MatchService', ['save', 'update']);
    teamService = jasmine.createSpyObj('TeamService', ['findAll']);
    refereeService = jasmine.createSpyObj('RefereeService', ['findAll']);
    gradeService = jasmine.createSpyObj('GradeService', ['findById', 'save', 'update', 'delete']);

    teamService.findAll.and.returnValue(of(teams));
    refereeService.findAll.and.returnValue(of(referees));

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
      expect(component.editMode).toBeFalse();
      expect(component.model).toEqual({} as Match);
      expect(gradeService.findById).not.toHaveBeenCalled();
    });

    it('copies the edited match into the form model so edits do not leak into the list', () => {
      const match = makeMatch();
      const component = createComponent(match).componentInstance;

      expect(component.editMode).toBeTrue();
      expect(component.model).toEqual(match);
      expect(component.model).not.toBe(match);

      component.model.queue = 99;
      expect(match.queue).toBe(3);
    });

    it('fetches the existing grade when the match references one', () => {
      const grade: Grade = {id: 5, value: 8.1};
      gradeService.findById.and.returnValue(of(grade));

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
      matchService.save.and.returnValue(of(saved));
      const emitted: Match[] = [];
      component.saved.subscribe(m => emitted.push(m));

      component.model = {queue: 3, homeTeamId: 1, awayTeamId: 2} as Match;
      component.onSubmit(validForm);

      expect(matchService.save).toHaveBeenCalledWith(component.model);
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(emitted).toEqual([saved]);
    });

    it('saves an entered grade against the newly created match before emitting', () => {
      const component = createComponent(null).componentInstance;
      const saved = makeMatch({id: 42});
      matchService.save.and.returnValue(of(saved));
      const gradeSave = new Subject<Grade>();
      gradeService.save.and.returnValue(gradeSave);
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
      matchService.update.and.returnValue(of(updated));
      emitted = [];
    });

    function createInEditMode(gradeId?: number, storedGrade?: Grade): void {
      if (storedGrade) gradeService.findById.and.returnValue(of(storedGrade));
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
      gradeService.update.and.returnValue(of({id: 5, value: 8.0}));

      component.grade.value = 8.0;
      component.onSubmit(validForm);

      expect(gradeService.update).toHaveBeenCalledWith(component.grade);
      expect(gradeService.save).not.toHaveBeenCalled();
      expect(gradeService.delete).not.toHaveBeenCalled();
      expect(emitted).toEqual([updated]);
    });

    it('creates the grade when one was entered for a match without one', () => {
      createInEditMode();
      gradeService.save.and.returnValue(of({id: 6, value: 8.2}));

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
      gradeService.delete.and.returnValue(gradeDelete);

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
});

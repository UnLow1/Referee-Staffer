import type {MockedObject} from 'vitest';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NgForm} from '@angular/forms';
import {of} from 'rxjs';
import {VacationFormComponent} from './vacation-form.component';
import {VacationService} from '../../service/vacation.service';
import {RefereeService} from '../../service/referee.service';
import {Vacation} from '../../model/vacation';
import {Referee} from '../../model/referee';
import {createMock} from '../../testing/mock';

describe('VacationFormComponent', () => {
  let vacationService: MockedObject<VacationService>;
  let refereeService: MockedObject<RefereeService>;

  const referees: Referee[] = [
    {id: 1, firstName: 'Zenon', lastName: 'Zielinski', email: 'z@example.com', experience: 5},
    {id: 2, firstName: 'Adam', lastName: 'Adamski', email: 'a@example.com', experience: 8}
  ];

  // Native date inputs bind ISO strings, not Date objects — mirror that in the fixture.
  const existing = {
    id: 4, refereeId: 2, startDate: '2026-08-01', endDate: '2026-08-14'
  } as unknown as Vacation;

  const validForm = {valid: true} as NgForm;

  beforeEach(async () => {
    vacationService = createMock<VacationService>(['save', 'update']);
    refereeService = createMock<RefereeService>(['findAll']);
    refereeService.findAll.mockReturnValue(of(referees));

    await TestBed.configureTestingModule({
      imports: [VacationFormComponent],
      providers: [
        {provide: VacationService, useValue: vacationService},
        {provide: RefereeService, useValue: refereeService}
      ]
    }).compileComponents();
  });

  function create(vacation: Vacation | null): ComponentFixture<VacationFormComponent> {
    const fixture = TestBed.createComponent(VacationFormComponent);
    fixture.componentInstance.vacation = vacation;
    fixture.detectChanges();
    return fixture;
  }

  it('sorts the referee select by last name', () => {
    const component = create(null).componentInstance;

    expect(component.referees.map(r => r.lastName)).toEqual(['Adamski', 'Zielinski']);
  });

  it('copies the edited vacation into the model', () => {
    const component = create(existing).componentInstance;

    expect(component.editMode).toBe(true);
    expect(component.model).toEqual({refereeId: 2, startDate: '2026-08-01', endDate: '2026-08-14'} as unknown as typeof component.model);
  });

  describe('endBeforeStart', () => {
    it('is false while either date is missing', () => {
      const component = create(null).componentInstance;
      expect(component.endBeforeStart).toBe(false);

      component.model.startDate = '2026-08-01' as unknown as Date;
      expect(component.endBeforeStart).toBe(false);
    });

    it('is true only when the end is strictly before the start', () => {
      const component = create(null).componentInstance;
      component.model.startDate = '2026-08-10' as unknown as Date;

      component.model.endDate = '2026-08-09' as unknown as Date;
      expect(component.endBeforeStart).toBe(true);

      // A one-day vacation (start == end) is valid.
      component.model.endDate = '2026-08-10' as unknown as Date;
      expect(component.endBeforeStart).toBe(false);
    });
  });

  describe('onSubmit', () => {
    it('ignores an invalid form', () => {
      create(null).componentInstance.onSubmit({valid: false} as NgForm);

      expect(vacationService.save).not.toHaveBeenCalled();
      expect(vacationService.update).not.toHaveBeenCalled();
    });

    it('blocks a reversed date range even when Angular validators pass', () => {
      const component = create(null).componentInstance;
      component.model = {
        refereeId: 1, startDate: '2026-08-10', endDate: '2026-08-01'
      } as unknown as typeof component.model;

      component.onSubmit(validForm);

      expect(vacationService.save).not.toHaveBeenCalled();
    });

    it('saves a new vacation and emits the backend response', () => {
      const component = create(null).componentInstance;
      const saved = {...existing, id: 77};
      vacationService.save.mockReturnValue(of(saved));
      const emitted: Vacation[] = [];
      component.saved.subscribe(v => emitted.push(v));

      component.model = {
        refereeId: 1, startDate: '2026-08-01', endDate: '2026-08-05'
      } as unknown as typeof component.model;
      component.onSubmit(validForm);

      expect(vacationService.save).toHaveBeenCalledWith(expect.objectContaining({refereeId: 1}));
      expect(emitted).toEqual([saved]);
    });

    it('updates on edit, keeping the id from the original vacation', () => {
      const component = create(existing).componentInstance;
      vacationService.update.mockReturnValue(of(existing));

      component.model.endDate = '2026-08-20' as unknown as Date;
      component.onSubmit(validForm);

      expect(vacationService.update).toHaveBeenCalledWith(expect.objectContaining({
        id: 4, refereeId: 2, endDate: '2026-08-20'
      }));
      expect(vacationService.save).not.toHaveBeenCalled();
    });
  });
});

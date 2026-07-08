import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, Router} from '@angular/router';
import {of} from 'rxjs';
import {VacationListComponent} from './vacation-list.component';
import {VacationService} from '../../service/vacation.service';
import {RefereeService} from '../../service/referee.service';
import {Vacation} from '../../model/vacation';
import {Referee} from '../../model/referee';

describe('VacationListComponent', () => {
  let vacationService: jasmine.SpyObj<VacationService>;
  let refereeService: jasmine.SpyObj<RefereeService>;
  let router: jasmine.SpyObj<Router>;

  const referees: Referee[] = [
    {id: 1, firstName: 'Jan', lastName: 'Kowalski', email: 'jan@example.com', experience: 10},
    {id: 2, firstName: 'Anna', lastName: 'Nowak', email: 'anna@example.com', experience: 20}
  ];

  /** yyyy-MM-dd in local time, offset by `days` from today. */
  function isoDay(offsetDays: number): string {
    const d = new Date();
    d.setDate(d.getDate() + offsetDays);
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${month}-${day}`;
  }

  function makeVacation(id: number, refereeId: number, startDate: string, endDate: string): Vacation {
    return {id, refereeId, startDate, endDate} as unknown as Vacation;
  }

  const past = makeVacation(31, 1, isoDay(-10), isoDay(-5));
  const active = makeVacation(32, 2, isoDay(-1), isoDay(1));
  const upcoming = makeVacation(33, 1, isoDay(5), isoDay(8));

  beforeEach(() => {
    vacationService = jasmine.createSpyObj('VacationService', ['findAll', 'findById', 'delete']);
    refereeService = jasmine.createSpyObj('RefereeService', ['findByIds', 'findAll']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    vacationService.findAll.and.returnValue(of([upcoming, past, active]));
    refereeService.findByIds.and.returnValue(of(referees));
    // The vacation form (rendered when the drawer opens) loads the pool on init.
    refereeService.findAll.and.returnValue(of(referees));
  });

  async function create(path = 'vacations', id?: number): Promise<ComponentFixture<VacationListComponent>> {
    await TestBed.configureTestingModule({
      imports: [VacationListComponent],
      providers: [
        {provide: VacationService, useValue: vacationService},
        {provide: RefereeService, useValue: refereeService},
        {provide: Router, useValue: router},
        {
          provide: ActivatedRoute,
          useValue: {snapshot: {url: [{path}], paramMap: convertToParamMap(id ? {id: String(id)} : {})}}
        }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(VacationListComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('sorts by start date and joins the referees once by their unique ids', async () => {
    const fixture = await create();
    const component = fixture.componentInstance;

    expect(component.sortedVacations().map(v => v.id)).toEqual([31, 32, 33]);
    // Referee 1 owns two windows — the lookup must deduplicate.
    expect(refereeService.findByIds).toHaveBeenCalledWith([1, 2]);
    expect(component.getReferee(2)?.lastName).toBe('Nowak');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(3);
  });

  it('clears the referee lookup when there are no vacations', async () => {
    vacationService.findAll.and.returnValue(of([]));

    const component = (await create()).componentInstance;

    expect(refereeService.findByIds).not.toHaveBeenCalled();
    expect(component.refereesById().size).toBe(0);
  });

  it('derives the status from today against the window', async () => {
    const component = (await create()).componentInstance;

    expect(component.status(past)).toBe('past');
    expect(component.status(active)).toBe('active');
    expect(component.status(upcoming)).toBe('upcoming');
    // Boundary days are inclusive — a window starting or ending today is active.
    expect(component.status(makeVacation(41, 1, isoDay(0), isoDay(3)))).toBe('active');
    expect(component.status(makeVacation(42, 1, isoDay(-3), isoDay(0)))).toBe('active');
  });

  it('counts days inclusively', async () => {
    const component = (await create()).componentInstance;

    expect(component.days(makeVacation(51, 1, '2026-08-01', '2026-08-01'))).toBe(1);
    expect(component.days(makeVacation(52, 1, '2026-08-01', '2026-08-14'))).toBe(14);
  });

  it('deletes only after the confirm, naming the owner in the guard', async () => {
    const component = (await create()).componentInstance;
    vacationService.delete.and.returnValue(of(void 0));

    component.askDelete(active);
    expect(component.deleteGuard().message).toContain("Anna Nowak's vacation");

    component.confirmDelete();

    expect(vacationService.delete).toHaveBeenCalledWith(32);
    expect(component.vacations().map(v => v.id)).not.toContain(32);
    expect(component.deleteTarget()).toBeNull();
  });

  describe('deep links', () => {
    it('opens an empty drawer for /addVacation', async () => {
      const component = (await create('addVacation')).componentInstance;

      expect(component.formOpen()).toBeTrue();
      expect(component.editingVacation()).toBeNull();
    });

    it('fetches the vacation and opens the edit drawer for /addVacation/:id', async () => {
      vacationService.findById.and.returnValue(of(past));

      const component = (await create('addVacation', 31)).componentInstance;

      expect(vacationService.findById).toHaveBeenCalledWith(31);
      expect(component.editingVacation()).toEqual(past);
      expect(component.formOpen()).toBeTrue();
    });

    it('normalizes the URL back to the list on close only when deep-linked', async () => {
      const component = (await create('addVacation')).componentInstance;

      component.closeForm();

      expect(router.navigate).toHaveBeenCalledWith(['/vacations']);
    });
  });
});

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, Router} from '@angular/router';
import {of} from 'rxjs';
import {RefereeListComponent} from './referee-list.component';
import {RefereeService} from '../../service/referee.service';
import {Referee} from '../../model/referee';

describe('RefereeListComponent', () => {
  let refereeService: jasmine.SpyObj<RefereeService>;
  let router: jasmine.SpyObj<Router>;

  const referees: Referee[] = [
    {id: 1, firstName: 'Jan', lastName: 'Kowalski', email: 'jan@example.com', experience: 10, potential: 70},
    {id: 2, firstName: 'Anna', lastName: 'Nowak', email: 'anna@example.com', experience: 20, potential: 90},
    // Un-enriched: no potential, ranks by experience.
    {id: 3, firstName: 'Piotr', lastName: 'Wisniewski', email: 'piotr@example.com', experience: 80}
  ];

  beforeEach(() => {
    refereeService = jasmine.createSpyObj('RefereeService', ['findAll', 'findById', 'delete']);
    refereeService.findAll.and.returnValue(of(referees));
    router = jasmine.createSpyObj('Router', ['navigate']);
  });

  async function create(path = 'referees', id?: number): Promise<ComponentFixture<RefereeListComponent>> {
    await TestBed.configureTestingModule({
      imports: [RefereeListComponent],
      providers: [
        {provide: RefereeService, useValue: refereeService},
        {provide: Router, useValue: router},
        {
          provide: ActivatedRoute,
          useValue: {snapshot: {url: [{path}], paramMap: convertToParamMap(id ? {id: String(id)} : {})}}
        }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(RefereeListComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('loads the pool and ranks by potential with experience fallback', async () => {
    const fixture = await create();

    // 90 (Nowak), 80-by-experience (Wisniewski), 70 (Kowalski).
    expect(fixture.componentInstance.visibleReferees().map(r => r.id)).toEqual([2, 3, 1]);
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(3);
  });

  it('filters by name or email', async () => {
    const component = (await create()).componentInstance;

    component.setSearch('nowak');
    expect(component.visibleReferees().map(r => r.id)).toEqual([2]);

    component.setSearch('piotr@');
    expect(component.visibleReferees().map(r => r.id)).toEqual([3]);

    component.setSearch('nikt taki');
    expect(component.visibleReferees()).toEqual([]);
  });

  it('scales the potential meter to the pool maximum', async () => {
    const component = (await create()).componentInstance;

    expect(component.maxPotential()).toBe(90);
    expect(component.potentialPct(referees[1])).toBe(100);
    expect(component.potentialPct(referees[0])).toBe(78); // 70/90
    expect(component.potentialPct(referees[2])).toBe(0);  // no potential
  });

  it('navigates to the profile on row click', async () => {
    const component = (await create()).componentInstance;

    component.openProfile(referees[0]);

    expect(router.navigate).toHaveBeenCalledWith(['/referees', 1]);
  });

  describe('delete flow', () => {
    it('asks for confirmation naming the referee, then deletes on confirm', async () => {
      const fixture = await create();
      const component = fixture.componentInstance;
      refereeService.delete.and.returnValue(of(void 0));

      component.askDelete(referees[1], new Event('click'));
      fixture.detectChanges();

      expect(component.deleteGuard().message).toContain('Anna Nowak');
      expect((fixture.nativeElement as HTMLElement).querySelector('.modal')).not.toBeNull();

      component.confirmDelete();

      expect(refereeService.delete).toHaveBeenCalledWith(2);
      expect(component.referees().map(r => r.id)).toEqual([1, 3]);
      expect(component.deleteTarget()).toBeNull();
    });

    it('does nothing when confirming without a target', async () => {
      (await create()).componentInstance.confirmDelete();

      expect(refereeService.delete).not.toHaveBeenCalled();
    });
  });

  describe('deep links', () => {
    it('opens an empty drawer for /addReferee', async () => {
      const component = (await create('addReferee')).componentInstance;

      expect(component.formOpen()).toBeTrue();
      expect(component.editingReferee()).toBeNull();
    });

    it('fetches the referee and opens the edit drawer for /addReferee/:id', async () => {
      refereeService.findById.and.returnValue(of(referees[0]));

      const component = (await create('addReferee', 1)).componentInstance;

      expect(refereeService.findById).toHaveBeenCalledWith(1);
      expect(component.formOpen()).toBeTrue();
      expect(component.editingReferee()).toEqual(referees[0]);
    });

    it('normalizes the URL back to the list when the deep-linked drawer closes', async () => {
      const component = (await create('addReferee')).componentInstance;

      component.closeForm();

      expect(component.formOpen()).toBeFalse();
      expect(router.navigate).toHaveBeenCalledWith(['/referees']);
    });

    it('does not touch the URL when closing a drawer opened from the list', async () => {
      const component = (await create()).componentInstance;
      component.addReferee();

      component.closeForm();

      expect(router.navigate).not.toHaveBeenCalled();
    });
  });

  it('re-fetches the enriched list after a save and closes the drawer', async () => {
    const component = (await create()).componentInstance;
    component.addReferee();
    refereeService.findAll.calls.reset();

    component.onSaved();

    expect(refereeService.findAll).toHaveBeenCalledTimes(1);
    expect(component.formOpen()).toBeFalse();
  });
});

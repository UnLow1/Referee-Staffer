import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {NgForm} from '@angular/forms';
import {of} from 'rxjs';
import {RefereeFormComponent} from './referee-form.component';
import {RefereeService} from '../../service/referee.service';
import {Referee} from '../../model/referee';

describe('RefereeFormComponent', () => {
  let refereeService: jasmine.SpyObj<RefereeService>;

  const existing: Referee = {
    id: 9,
    firstName: 'Jan',
    lastName: 'Kowalski',
    email: 'jan@example.com',
    experience: 12,
    // Enriched stats never shown in the form — they must survive an edit untouched.
    averageGrade: 8.2,
    potential: 88,
    lastQueue: 24
  };

  const validForm = {valid: true} as NgForm;

  beforeEach(async () => {
    refereeService = jasmine.createSpyObj('RefereeService', ['save', 'update']);
    await TestBed.configureTestingModule({
      imports: [RefereeFormComponent],
      providers: [{provide: RefereeService, useValue: refereeService}]
    }).compileComponents();
  });

  function create(referee: Referee | null): ComponentFixture<RefereeFormComponent> {
    const fixture = TestBed.createComponent(RefereeFormComponent);
    fixture.componentInstance.referee = referee;
    fixture.detectChanges();
    return fixture;
  }

  it('starts empty in add mode', () => {
    const component = create(null).componentInstance;

    expect(component.editMode).toBeFalse();
    expect(component.subtitle).toBe('New official in the pool');
    expect(component.model.firstName).toBe('');
  });

  it('copies only the editable fields in edit mode', () => {
    const component = create(existing).componentInstance;

    expect(component.editMode).toBeTrue();
    expect(component.subtitle).toBe('Jan Kowalski');
    expect(component.model).toEqual({
      firstName: 'Jan', lastName: 'Kowalski', email: 'jan@example.com', experience: 12
    });
  });

  it('ignores submit while the form is invalid', () => {
    create(null).componentInstance.onSubmit({valid: false} as NgForm);

    expect(refereeService.save).not.toHaveBeenCalled();
    expect(refereeService.update).not.toHaveBeenCalled();
  });

  it('saves a new referee and emits the backend response', () => {
    const component = create(null).componentInstance;
    const saved = {...existing, id: 55};
    refereeService.save.and.returnValue(of(saved));
    const emitted: Referee[] = [];
    component.saved.subscribe(r => emitted.push(r));

    component.model = {firstName: 'Anna', lastName: 'Nowak', email: 'anna@example.com', experience: 3};
    component.onSubmit(validForm);

    expect(refereeService.save).toHaveBeenCalledWith(jasmine.objectContaining({
      firstName: 'Anna', lastName: 'Nowak', email: 'anna@example.com', experience: 3
    }));
    expect(refereeService.update).not.toHaveBeenCalled();
    expect(emitted).toEqual([saved]);
  });

  it('updates on edit with the enriched stats riding along in the payload', () => {
    const component = create(existing).componentInstance;
    refereeService.update.and.returnValue(of(existing));

    component.model.experience = 13;
    component.onSubmit(validForm);

    expect(refereeService.update).toHaveBeenCalledWith(jasmine.objectContaining({
      id: 9, experience: 13, averageGrade: 8.2, potential: 88, lastQueue: 24
    }));
    expect(refereeService.save).not.toHaveBeenCalled();
  });

  it('shows validation errors only after a field is touched or dirtied', fakeAsync(() => {
    const fixture = create(null);
    tick();
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    // All fields are empty and invalid, but pristine — no errors yet.
    expect(el.querySelectorAll('.err').length).toBe(0);

    // Dirty the first-name field, then empty it again.
    const firstName = el.querySelector('#firstName') as HTMLInputElement;
    firstName.value = 'J';
    firstName.dispatchEvent(new Event('input'));
    firstName.value = '';
    firstName.dispatchEvent(new Event('input'));
    tick();
    fixture.detectChanges();

    expect(el.querySelectorAll('.err').length).toBe(1);
    expect(el.querySelector('.err')?.textContent).toContain('First name is required');
  }));

  it('rejects an email without a TLD via the pattern and keeps submit disabled until all fields are valid', fakeAsync(() => {
    const fixture = create(null);
    tick();
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    const submit = el.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(submit.disabled).toBeTrue();

    function type(selector: string, value: string): void {
      const input = el.querySelector(selector) as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }

    type('#firstName', 'Jan');
    type('#lastName', 'Kowalski');
    type('#experience', '5');
    // Angular's email validator would accept this; the stricter pattern must not.
    type('#email', 'jan@localhost');
    tick();
    fixture.detectChanges();
    expect(submit.disabled).toBeTrue();

    type('#email', 'jan@example.com');
    tick();
    fixture.detectChanges();
    expect(submit.disabled).toBeFalse();
  }));
});

import type {MockedObject} from 'vitest';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NgForm} from '@angular/forms';
import {of} from 'rxjs';
import {TeamFormComponent} from './team-form.component';
import {TeamService} from '../../service/team.service';
import {SHORT_CODE_MAX_LENGTH, Team} from '../../model/team';
import {createMock} from '../../testing/mock';

describe('TeamFormComponent', () => {
  let teamService: MockedObject<TeamService>;

  const existing: Team = {id: 3, name: 'Alfa', city: 'Krakow', points: 40, short: 'ALF'};
  const validForm = {valid: true} as NgForm;

  beforeEach(async () => {
    teamService = createMock<TeamService>(['save', 'update']);
    await TestBed.configureTestingModule({
      imports: [TeamFormComponent],
      providers: [{provide: TeamService, useValue: teamService}]
    }).compileComponents();
  });

  function create(team: Team | null): ComponentFixture<TeamFormComponent> {
    const fixture = TestBed.createComponent(TeamFormComponent);
    fixture.componentInstance.team = team;
    fixture.detectChanges();
    return fixture;
  }

  it('starts empty in add mode', () => {
    const component = create(null).componentInstance;

    expect(component.editMode).toBe(false);
    expect(component.subtitle).toBe('New club in the league');
    expect(component.model).toEqual({name: '', city: '', shortOverride: ''});
  });

  it('copies name, city and the stored override in edit mode', () => {
    const component = create(existing).componentInstance;

    expect(component.editMode).toBe(true);
    expect(component.subtitle).toBe('Alfa');
    expect(component.model).toEqual({name: 'Alfa', city: 'Krakow', shortOverride: ''});
  });

  it('pre-fills the short code field from the stored override, not the computed one', () => {
    const component = create({...existing, short: 'ALF', shortOverride: 'AFA'}).componentInstance;

    expect(component.model.shortOverride).toBe('AFA');
  });

  it('ignores submit while the form is invalid', () => {
    create(null).componentInstance.onSubmit({valid: false} as NgForm);

    expect(teamService.save).not.toHaveBeenCalled();
    expect(teamService.update).not.toHaveBeenCalled();
  });

  it('saves a new team and emits the backend response', () => {
    const component = create(null).componentInstance;
    const saved: Team = {id: 10, name: 'Beta', city: 'Gdansk', points: 0};
    teamService.save.mockReturnValue(of(saved));
    const emitted: Team[] = [];
    component.saved.subscribe(t => emitted.push(t));

    component.model = {name: 'Beta', city: 'Gdansk', shortOverride: ''};
    component.onSubmit(validForm);

    expect(teamService.save).toHaveBeenCalledWith(expect.objectContaining({name: 'Beta', city: 'Gdansk'}));
    expect(emitted).toEqual([saved]);
  });

  it('updates on edit with the backend-owned fields riding along in the payload', () => {
    const component = create(existing).componentInstance;
    teamService.update.mockReturnValue(of(existing));

    component.model.city = 'Wieliczka';
    component.onSubmit(validForm);

    expect(teamService.update).toHaveBeenCalledWith(expect.objectContaining({
      id: 3, name: 'Alfa', city: 'Wieliczka', points: 40, short: 'ALF'
    }));
    expect(teamService.save).not.toHaveBeenCalled();
  });

  it('submits a hand-typed short code normalised to upper case', () => {
    const component = create(existing).componentInstance;
    teamService.update.mockReturnValue(of(existing));

    component.model.shortOverride = ' afa ';
    component.onSubmit(validForm);

    expect(teamService.update).toHaveBeenCalledWith(expect.objectContaining({shortOverride: 'AFA'}));
  });

  it('submits null when the short code field is left blank so the backend clears the override', () => {
    const component = create({...existing, shortOverride: 'AFA'}).componentInstance;
    teamService.update.mockReturnValue(of(existing));

    component.model.shortOverride = '   ';
    component.onSubmit(validForm);

    expect(teamService.update).toHaveBeenCalledWith(expect.objectContaining({shortOverride: null}));
  });

  it('never submits the computed short field as the override', () => {
    const component = create(existing).componentInstance;
    teamService.update.mockReturnValue(of(existing));

    component.onSubmit(validForm);

    // `short` rides along untouched, but the writable half stays null — otherwise the
    // name-derived fallback would be persisted and frozen against later renames.
    const payload = teamService.update.mock.calls[0][0];
    expect(payload.short).toBe('ALF');
    expect(payload.shortOverride).toBeNull();
  });

  it('derives the placeholder code from the typed name, skipping separators', () => {
    const component = create(null).componentInstance;

    component.model.name = 'FC Barcelona';

    expect(component.derivedShort).toBe('FCB');
  });

  it('renders the short code input bound to the override', () => {
    const fixture = create({...existing, shortOverride: 'AFA'});
    const input: HTMLInputElement = fixture.nativeElement.querySelector('#short');

    expect(input).toBeTruthy();
    expect(input.value).toBe('AFA');
    expect(input.required).toBe(false);
  });

  // The specs below type into the real input and read the form's own validity, so the
  // template validators actually run. Asserting on a {valid: true} NgForm stub cannot catch
  // a broken `pattern`, which is how an unusable field once passed a green suite.
  async function typeShortCode(fixture: ComponentFixture<TeamFormComponent>, value: string): Promise<NgForm> {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('#short');
    input.value = value;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    fixture.detectChanges();
    return formOf(fixture);
  }

  function formOf(fixture: ComponentFixture<TeamFormComponent>): NgForm {
    return fixture.debugElement.query(node => node.name === 'form').injector.get(NgForm);
  }

  it.each([
    ['LGA', true],
    ['ŁKS', true],
    ['LG1', true],
    ['', true],
    ['LG-', false],
    ['LG A', false]
  ])('treats a short code of %o as valid=%o', async (value, expected) => {
    const fixture = create(existing);

    const form = await typeShortCode(fixture, value as string);

    expect((fixture.nativeElement.querySelector('#short') as HTMLInputElement).value).toBe(value);
    expect(form.controls['short'].valid).toBe(expected);
  });

  it('keeps the whole form submittable after typing a plain code', async () => {
    const fixture = create(existing);

    const form = await typeShortCode(fixture, 'LGA');

    // The drawer gates its submit button on form validity, so an invalid pattern here would
    // make the drawer silently unsavable — including for a plain name or city edit.
    expect(form.valid).toBe(true);
  });

  it('caps the short code input at the length the team pill can render', () => {
    const input: HTMLInputElement = create(existing).nativeElement.querySelector('#short');

    expect(input.maxLength).toBe(SHORT_CODE_MAX_LENGTH);
  });

  it('opens valid for an imported team that already carries an override', async () => {
    const fixture = create({...existing, short: 'LECH', shortOverride: 'LECH'});
    await fixture.whenStable();
    fixture.detectChanges();

    expect(formOf(fixture).valid).toBe(true);
  });
});

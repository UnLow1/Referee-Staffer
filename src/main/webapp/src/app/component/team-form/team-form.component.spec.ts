import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NgForm} from '@angular/forms';
import {of} from 'rxjs';
import {TeamFormComponent} from './team-form.component';
import {TeamService} from '../../service/team.service';
import {Team} from '../../model/team';

describe('TeamFormComponent', () => {
  let teamService: jasmine.SpyObj<TeamService>;

  const existing: Team = {id: 3, name: 'Alfa', city: 'Krakow', points: 40, short: 'ALF'};
  const validForm = {valid: true} as NgForm;

  beforeEach(async () => {
    teamService = jasmine.createSpyObj('TeamService', ['save', 'update']);
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

    expect(component.editMode).toBeFalse();
    expect(component.subtitle).toBe('New club in the league');
    expect(component.model).toEqual({name: '', city: ''});
  });

  it('copies only name and city in edit mode', () => {
    const component = create(existing).componentInstance;

    expect(component.editMode).toBeTrue();
    expect(component.subtitle).toBe('Alfa');
    expect(component.model).toEqual({name: 'Alfa', city: 'Krakow'});
  });

  it('ignores submit while the form is invalid', () => {
    create(null).componentInstance.onSubmit({valid: false} as NgForm);

    expect(teamService.save).not.toHaveBeenCalled();
    expect(teamService.update).not.toHaveBeenCalled();
  });

  it('saves a new team and emits the backend response', () => {
    const component = create(null).componentInstance;
    const saved: Team = {id: 10, name: 'Beta', city: 'Gdansk', points: 0};
    teamService.save.and.returnValue(of(saved));
    const emitted: Team[] = [];
    component.saved.subscribe(t => emitted.push(t));

    component.model = {name: 'Beta', city: 'Gdansk'};
    component.onSubmit(validForm);

    expect(teamService.save).toHaveBeenCalledWith(jasmine.objectContaining({name: 'Beta', city: 'Gdansk'}));
    expect(emitted).toEqual([saved]);
  });

  it('updates on edit with the backend-owned fields riding along in the payload', () => {
    const component = create(existing).componentInstance;
    teamService.update.and.returnValue(of(existing));

    component.model.city = 'Wieliczka';
    component.onSubmit(validForm);

    expect(teamService.update).toHaveBeenCalledWith(jasmine.objectContaining({
      id: 3, name: 'Alfa', city: 'Wieliczka', points: 40, short: 'ALF'
    }));
    expect(teamService.save).not.toHaveBeenCalled();
  });
});

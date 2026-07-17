import {ComponentFixture, TestBed} from '@angular/core/testing';
import {of, Subject, throwError} from 'rxjs';
import {ConfigurationComponent} from './configuration.component';
import {ConfigurationService} from '../../service/configuration.service';
import {Config} from '../../model/config';

describe('ConfigurationComponent', () => {
  let configurationService: jasmine.SpyObj<ConfigurationService>;

  const configs: Config[] = [
    {id: 1, name: 'GRADE_MULTIPLIER', value: 50, group: 'potential', description: 'Weight of the grade'},
    {id: 2, name: 'DERBY_INCREMENT', value: 10, group: 'difficulty'},
    {id: 3, name: 'QUEUE_PENALTY', value: 2, group: 'effective'},
    // Unknown group — must land in "effective" instead of disappearing.
    {id: 4, name: 'FUTURE_KNOB', value: 1, group: 'brand-new'},
    {id: 5, name: 'GROUPLESS_KNOB', value: 7}
  ];

  beforeEach(async () => {
    configurationService = jasmine.createSpyObj('ConfigurationService', ['findAll', 'update']);
    configurationService.findAll.and.returnValue(of(configs));

    await TestBed.configureTestingModule({
      imports: [ConfigurationComponent],
      providers: [
        {provide: ConfigurationService, useValue: configurationService}
      ]
    }).compileComponents();
  });

  function create(): ComponentFixture<ConfigurationComponent> {
    const fixture = TestBed.createComponent(ConfigurationComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('groups configs by the backend group, defaulting unknown groups to effective', () => {
    const component = create().componentInstance;

    expect(component.configsFor('potential').map(c => c.name)).toEqual(['GRADE_MULTIPLIER']);
    expect(component.configsFor('difficulty').map(c => c.name)).toEqual(['DERBY_INCREMENT']);
    expect(component.configsFor('effective').map(c => c.name))
      .toEqual(['QUEUE_PENALTY', 'FUTURE_KNOB', 'GROUPLESS_KNOB']);
  });

  it('adopts the fetched values as both working copy and reset baseline', () => {
    const component = create().componentInstance;

    expect(component.valueFor('GRADE_MULTIPLIER')).toBe(50);
    expect(component.defaultFor('GRADE_MULTIPLIER')).toBe(50);
    expect(component.savedAt()).toBeNull();
  });

  it('parses edited values and ignores input that is not a number', () => {
    const component = create().componentInstance;

    component.setValue('GRADE_MULTIPLIER', '62.5');
    expect(component.valueFor('GRADE_MULTIPLIER')).toBe(62.5);
    // The baseline is untouched — that's what Reset reverts to.
    expect(component.defaultFor('GRADE_MULTIPLIER')).toBe(50);

    component.setValue('GRADE_MULTIPLIER', 'not-a-number');
    expect(component.valueFor('GRADE_MULTIPLIER')).toBe(62.5);
  });

  it('reverts every edit on reset', () => {
    const component = create().componentInstance;

    component.setValue('GRADE_MULTIPLIER', 62.5);
    component.setValue('DERBY_INCREMENT', 11);
    component.reset();

    expect(component.valueFor('GRADE_MULTIPLIER')).toBe(50);
    expect(component.valueFor('DERBY_INCREMENT')).toBe(10);
  });

  it('saves the edited values and adopts the refreshed response', () => {
    const response = new Subject<Config[]>();
    configurationService.update.and.returnValue(response);
    const component = create().componentInstance;

    component.setValue('GRADE_MULTIPLIER', 62.5);
    component.save();

    expect(component.saving()).toBeTrue();
    const sent = configurationService.update.calls.mostRecent().args[0];
    expect(sent.find(c => c.name === 'GRADE_MULTIPLIER')?.value).toBe(62.5);
    expect(sent.find(c => c.name === 'DERBY_INCREMENT')?.value).toBe(10);

    const refreshed = configs.map(c => c.name === 'GRADE_MULTIPLIER' ? {...c, value: 62.5} : c);
    response.next(refreshed);
    response.complete();

    expect(component.saving()).toBeFalse();
    expect(component.savedAt()).not.toBeNull();
    // The refreshed values are the new reset baseline.
    expect(component.defaultFor('GRADE_MULTIPLIER')).toBe(62.5);
  });

  it('clears the saved marker as soon as a value changes again', () => {
    configurationService.update.and.returnValue(of(configs));
    const component = create().componentInstance;

    component.save();
    expect(component.savedAt()).not.toBeNull();

    component.setValue('QUEUE_PENALTY', 3);
    expect(component.savedAt()).toBeNull();
  });

  it('stops the saving indicator when the update fails', () => {
    configurationService.update.and.returnValue(throwError(() => new Error('boom')));
    const component = create().componentInstance;

    component.save();

    expect(component.saving()).toBeFalse();
    expect(component.savedAt()).toBeNull();
  });
});

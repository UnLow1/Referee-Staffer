import {TestBed} from '@angular/core/testing';
import {UiSettingsService} from './ui-settings.service';

describe('UiSettingsService', () => {
  function reset(): void {
    localStorage.removeItem('theme');
    localStorage.removeItem('admin.visible');
    localStorage.removeItem('staffer.explainer');
    document.documentElement.removeAttribute('data-theme');
  }

  // afterEach too: the toggles write localStorage and stamp <html>, and that state
  // must not leak into whatever suite runs next.
  beforeEach(reset);
  afterEach(reset);

  // The constructor reads localStorage, so each test injects the service itself
  // after arranging the persisted state.
  function inject(): UiSettingsService {
    return TestBed.inject(UiSettingsService);
  }

  it('defaults everything off with a clean localStorage', () => {
    const service = inject();

    expect(service.dark()).toBeFalse();
    expect(service.adminVisible()).toBeFalse();
    expect(service.explainerVisible()).toBeFalse();
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('restores persisted settings on startup', () => {
    localStorage.setItem('theme', 'dark');
    localStorage.setItem('admin.visible', 'true');
    localStorage.setItem('staffer.explainer', 'true');

    const service = inject();

    expect(service.dark()).toBeTrue();
    expect(service.adminVisible()).toBeTrue();
    expect(service.explainerVisible()).toBeTrue();
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('toggleDark flips the signal, persists the theme and restamps <html>', () => {
    const service = inject();

    service.toggleDark();
    expect(service.dark()).toBeTrue();
    expect(localStorage.getItem('theme')).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');

    service.toggleDark();
    expect(service.dark()).toBeFalse();
    expect(localStorage.getItem('theme')).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('toggleAdmin flips the signal and persists it', () => {
    const service = inject();

    service.toggleAdmin();
    expect(service.adminVisible()).toBeTrue();
    expect(localStorage.getItem('admin.visible')).toBe('true');

    service.toggleAdmin();
    expect(service.adminVisible()).toBeFalse();
    expect(localStorage.getItem('admin.visible')).toBe('false');
  });

  it('toggleExplainer flips the signal and persists it', () => {
    const service = inject();

    service.toggleExplainer();
    expect(service.explainerVisible()).toBeTrue();
    expect(localStorage.getItem('staffer.explainer')).toBe('true');

    service.toggleExplainer();
    expect(service.explainerVisible()).toBeFalse();
    expect(localStorage.getItem('staffer.explainer')).toBe('false');
  });
});

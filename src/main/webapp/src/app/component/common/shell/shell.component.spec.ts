import {ComponentFixture, TestBed} from '@angular/core/testing';
import {signal, WritableSignal} from '@angular/core';
import {provideRouter} from '@angular/router';
import {ShellComponent} from './shell.component';
import {UiSettingsService} from '../../../service/ui-settings.service';

describe('ShellComponent', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let settings: {
    dark: WritableSignal<boolean>;
    adminVisible: WritableSignal<boolean>;
    explainerVisible: WritableSignal<boolean>;
    toggleDark: jasmine.Spy;
    toggleAdmin: jasmine.Spy;
    toggleExplainer: jasmine.Spy;
  };

  beforeEach(async () => {
    settings = {
      dark: signal(false),
      adminVisible: signal(false),
      explainerVisible: signal(false),
      toggleDark: jasmine.createSpy('toggleDark'),
      toggleAdmin: jasmine.createSpy('toggleAdmin'),
      toggleExplainer: jasmine.createSpy('toggleExplainer')
    };

    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([]),
        {provide: UiSettingsService, useValue: settings}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
  });

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function navLabels(): string[] {
    return Array.from(el().querySelectorAll('a.nav-item'))
      .map(a => a.textContent?.trim() ?? '');
  }

  it('renders the workspace, data and setup groups', () => {
    expect(navLabels()).toEqual([
      'Overview', 'Staffer',
      'Matches', 'Referees', 'Grades',
      'Import data', 'Configuration'
    ]);
  });

  it('hides the admin group until it is revealed', () => {
    expect(navLabels()).not.toContain('Teams');
    expect(el().querySelector('.nav-group--admin')).toBeNull();

    settings.adminVisible.set(true);
    fixture.detectChanges();

    expect(el().querySelector('.nav-group--admin')).not.toBeNull();
    expect(navLabels()).toEqual(jasmine.arrayContaining(['Teams', 'Standings', 'Vacations']));
  });

  it('labels the temporary admin toggle by the current state', () => {
    const toggle = el().querySelector('.sidenav__admin-toggle') as HTMLButtonElement;
    expect(toggle.textContent).toContain('Show admin section');

    settings.adminVisible.set(true);
    fixture.detectChanges();
    expect(toggle.textContent).toContain('Hide admin section');

    toggle.click();
    expect(settings.toggleAdmin).toHaveBeenCalled();
  });

  it('hosts the algorithm-explainer toggle inside the admin group', () => {
    expect(el().querySelector('.nav-item--toggle')).toBeNull();

    settings.adminVisible.set(true);
    fixture.detectChanges();

    const toggle = el().querySelector('.nav-item--toggle') as HTMLButtonElement;
    expect(toggle.textContent).toContain('Algorithm explainer');
    toggle.click();
    expect(settings.toggleExplainer).toHaveBeenCalled();
  });

  it('flips the theme from the topbar button', () => {
    (el().querySelector('.topbar__theme') as HTMLButtonElement).click();

    expect(settings.toggleDark).toHaveBeenCalled();
  });

  describe('mobile off-canvas nav', () => {
    function sidenav(): HTMLElement {
      return el().querySelector('.sidenav') as HTMLElement;
    }

    function hamburger(): HTMLButtonElement {
      return el().querySelector('.topbar__menu') as HTMLButtonElement;
    }

    it('toggles the nav and its backdrop from the hamburger', () => {
      expect(sidenav().classList).not.toContain('sidenav--open');
      expect(el().querySelector('.sidenav-backdrop')).toBeNull();

      hamburger().click();
      fixture.detectChanges();
      expect(sidenav().classList).toContain('sidenav--open');
      expect(el().querySelector('.sidenav-backdrop')).not.toBeNull();
      expect(hamburger().getAttribute('aria-expanded')).toBe('true');

      hamburger().click();
      fixture.detectChanges();
      expect(sidenav().classList).not.toContain('sidenav--open');
      expect(el().querySelector('.sidenav-backdrop')).toBeNull();
    });

    it('closes the nav from the backdrop', () => {
      hamburger().click();
      fixture.detectChanges();

      (el().querySelector('.sidenav-backdrop') as HTMLButtonElement).click();
      fixture.detectChanges();

      expect(sidenav().classList).not.toContain('sidenav--open');
    });

    it('closes the nav when a nav item is clicked', () => {
      hamburger().click();
      fixture.detectChanges();

      // ctrlKey makes RouterLink skip the actual navigation (browser would open a new
      // tab), so this exercises only the (click) handler without needing route config.
      el().querySelector('a.nav-item')!
        .dispatchEvent(new MouseEvent('click', {ctrlKey: true}));
      fixture.detectChanges();

      expect(sidenav().classList).not.toContain('sidenav--open');
    });

    it('closes the nav on Escape', () => {
      hamburger().click();
      fixture.detectChanges();

      document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape'}));
      fixture.detectChanges();

      expect(sidenav().classList).not.toContain('sidenav--open');
    });
  });
});

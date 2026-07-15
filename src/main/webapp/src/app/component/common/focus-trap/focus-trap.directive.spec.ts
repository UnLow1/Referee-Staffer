import {Component, signal} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FocusTrapDirective} from './focus-trap.directive';

@Component({
  imports: [FocusTrapDirective],
  template: `
    <button id="trigger" type="button">open</button>
    @if (open()) {
      <div id="panel" appFocusTrap>
        <button id="first" type="button">first</button>
        <button id="second" type="button" [disabled]="secondDisabled()">second</button>
        <button id="last" type="button">last</button>
        <button id="invisible" type="button" style="display: none">invisible</button>
      </div>
    }
    @if (stacked()) {
      <div id="stacked-panel" appFocusTrap>
        <button id="stacked-only" type="button">stacked</button>
      </div>
    }
  `
})
class HostComponent {
  readonly open = signal(false);
  readonly stacked = signal(false);
  readonly secondDisabled = signal(false);
}

describe('FocusTrapDirective', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({imports: [HostComponent]}).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  function el(id: string): HTMLElement {
    return (fixture.nativeElement as HTMLElement).querySelector(`#${id}`) as HTMLElement;
  }

  function pressTab(shiftKey = false): void {
    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab', shiftKey}));
  }

  function openTrap(): void {
    el('trigger').focus();
    host.open.set(true);
    fixture.detectChanges();
  }

  it('moves focus to the first focusable element on activation', () => {
    openTrap();
    expect(document.activeElement).toBe(el('first'));
  });

  it('wraps Tab from the last element back to the first', () => {
    openTrap();
    el('last').focus();

    pressTab();

    expect(document.activeElement).toBe(el('first'));
  });

  it('wraps Shift+Tab from the first element back to the last', () => {
    openTrap();

    pressTab(true);

    expect(document.activeElement).toBe(el('last'));
  });

  it('leaves Tab alone between inner elements', () => {
    openTrap();
    // A synthetic Tab does not natively move focus; the trap must not hijack it either.
    pressTab();
    expect(document.activeElement).toBe(el('first'));
  });

  it('skips disabled controls when wrapping', () => {
    host.secondDisabled.set(true);
    openTrap();
    el('first').focus();

    pressTab(true);

    expect(document.activeElement).toBe(el('last'));
  });

  it('pulls focus back inside when it escaped the container', () => {
    openTrap();
    el('trigger').focus();

    pressTab();

    expect(document.activeElement).toBe(el('first'));
  });

  it('returns focus to the trigger on deactivation', () => {
    openTrap();
    el('last').focus();

    host.open.set(false);
    fixture.detectChanges();

    expect(document.activeElement).toBe(el('trigger'));
  });

  it('lets only the topmost trap handle Tab and restores focus into the outer trap', () => {
    openTrap();
    el('last').focus();
    host.stacked.set(true);
    fixture.detectChanges();

    expect(document.activeElement).toBe(el('stacked-only'));
    pressTab();
    expect(document.activeElement).toBe(el('stacked-only'));

    host.stacked.set(false);
    fixture.detectChanges();

    expect(document.activeElement).toBe(el('last'));
    pressTab();
    expect(document.activeElement).toBe(el('first'));
  });

  it('skips the focus restore when the saved element is no longer in the DOM', () => {
    openTrap();
    host.stacked.set(true);
    fixture.detectChanges();

    // Close both at once — the inner trap's saved element (inside the outer panel) is
    // detached by the time it restores, so the outer trap's restore must win.
    host.open.set(false);
    host.stacked.set(false);
    fixture.detectChanges();

    expect(document.activeElement).toBe(el('trigger'));
  });
});

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormDrawerComponent} from './form-drawer.component';

describe('FormDrawerComponent', () => {
  let fixture: ComponentFixture<FormDrawerComponent>;
  let component: FormDrawerComponent;
  let closedCount: number;

  beforeEach(async () => {
    await TestBed.configureTestingModule({imports: [FormDrawerComponent]}).compileComponents();
    fixture = TestBed.createComponent(FormDrawerComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('open', true);
    fixture.componentRef.setInput('title', 'Add referee');
    fixture.componentRef.setInput('formId', 'referee-form');
    closedCount = 0;
    component.closed.subscribe(() => closedCount++);
    fixture.detectChanges();
  });

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function clickCancel(): void {
    const buttons = el().querySelectorAll<HTMLButtonElement>('.fdrawer__actions .btn');
    buttons[0].click();
    fixture.detectChanges();
  }

  function guard(): HTMLElement | null {
    return el().querySelector('.modal');
  }

  it('wires the footer submit button to the projected form and gates it on validity', () => {
    const submit = el().querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(submit.getAttribute('form')).toBe('referee-form');
    expect(submit.disabled).toBeTrue();

    fixture.componentRef.setInput('valid', true);
    fixture.detectChanges();
    expect(submit.disabled).toBeFalse();
  });

  it('shows the dirty indicator', () => {
    expect(el().querySelector('.fdrawer__dirty')?.textContent).toContain('No changes');

    fixture.componentRef.setInput('dirty', true);
    fixture.detectChanges();
    expect(el().querySelector('.fdrawer__dirty')?.textContent).toContain('Unsaved changes');
  });

  it('closes immediately while pristine', () => {
    clickCancel();

    expect(closedCount).toBe(1);
    expect(guard()).toBeNull();
  });

  it('routes a dirty close through the discard guard instead of closing', () => {
    fixture.componentRef.setInput('dirty', true);
    fixture.detectChanges();

    clickCancel();

    expect(closedCount).toBe(0);
    expect(guard()).not.toBeNull();
    expect(guard()?.textContent).toContain('Discard changes?');
  });

  it('closes once the discard guard is confirmed', () => {
    fixture.componentRef.setInput('dirty', true);
    fixture.detectChanges();
    clickCancel();

    (guard()!.querySelector('.modal__foot .btn--primary') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(closedCount).toBe(1);
    expect(guard()).toBeNull();
  });

  it('keeps the drawer open when the discard guard is dismissed', () => {
    fixture.componentRef.setInput('dirty', true);
    fixture.detectChanges();
    clickCancel();

    (guard()!.querySelector('.modal__foot .btn:not(.btn--primary)') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(closedCount).toBe(0);
    expect(guard()).toBeNull();
    expect(el().querySelector('.drawer')).not.toBeNull();
  });

  it('moves focus into the discard guard and back into the drawer when it is dismissed', () => {
    fixture.componentRef.setInput('dirty', true);
    fixture.detectChanges();
    const drawerClose = el().querySelector('button[aria-label="Close"]') as HTMLButtonElement;
    drawerClose.focus();

    clickCancel();
    expect(guard()!.contains(document.activeElement)).toBeTrue();

    (guard()!.querySelector('.modal__foot .btn:not(.btn--primary)') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(document.activeElement).toBe(drawerClose);
  });

  it('lets Escape dismiss only the guard while it is open, not the drawer behind it', () => {
    fixture.componentRef.setInput('dirty', true);
    fixture.detectChanges();
    clickCancel();
    expect(guard()).not.toBeNull();

    // Both the drawer and the dialog listen on document:keydown.escape. The drawer's
    // tryClose() must no-op while the guard is up, and the dialog's cancel closes it.
    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape'}));
    fixture.detectChanges();

    expect(guard()).toBeNull();
    expect(closedCount).toBe(0);
    expect(el().querySelector('.drawer')).not.toBeNull();
  });
});

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ConfirmDialogComponent} from './confirm-dialog.component';
import {ModalData} from '../../../model/modalData';

describe('ConfirmDialogComponent', () => {
  let fixture: ComponentFixture<ConfirmDialogComponent>;
  let component: ConfirmDialogComponent;
  let confirmedCount: number;
  let cancelledCount: number;

  const data: ModalData = {
    header: 'Delete referee?',
    message: 'This cannot be undone.'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({imports: [ConfirmDialogComponent]}).compileComponents();
    fixture = TestBed.createComponent(ConfirmDialogComponent);
    component = fixture.componentInstance;
    component.data = data;
    confirmedCount = 0;
    cancelledCount = 0;
    component.confirmed.subscribe(() => confirmedCount++);
    component.cancelled.subscribe(() => cancelledCount++);
  });

  function open(override: Partial<ModalData> = {}): HTMLElement {
    fixture.componentRef.setInput('data', {...data, ...override});
    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders nothing while closed', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.modal')).toBeNull();
  });

  it('renders header and message with default button labels', () => {
    const el = open();

    expect(el.querySelector('.modal__title')?.textContent).toContain('Delete referee?');
    expect(el.querySelector('.modal__body')?.textContent).toContain('This cannot be undone.');
    expect(el.querySelector('.modal__foot .btn:not(.btn--primary)')?.textContent).toContain('Cancel');
    expect(el.querySelector('.modal__foot .btn--primary')?.textContent).toContain('Confirm');
  });

  it('renders custom button labels', () => {
    const el = open({confirmLabel: 'Discard', cancelLabel: 'Keep editing'});

    expect(el.querySelector('.modal__foot .btn:not(.btn--primary)')?.textContent).toContain('Keep editing');
    expect(el.querySelector('.modal__foot .btn--primary')?.textContent).toContain('Discard');
  });

  it('styles the primary button as danger by default but not for warn tone', () => {
    let el = open();
    expect(el.querySelector('.btn--primary-danger')).not.toBeNull();

    fixture.componentRef.setInput('open', false);
    fixture.detectChanges();
    el = open({tone: 'warn'});
    expect(el.querySelector('.btn--primary-danger')).toBeNull();
  });

  it('emits confirmed on the primary button', () => {
    const el = open();
    (el.querySelector('.modal__foot .btn--primary') as HTMLButtonElement).click();

    expect(confirmedCount).toBe(1);
    expect(cancelledCount).toBe(0);
  });

  it('emits cancelled on the cancel button', () => {
    const el = open();
    (el.querySelector('.modal__foot .btn:not(.btn--primary)') as HTMLButtonElement).click();

    expect(cancelledCount).toBe(1);
    expect(confirmedCount).toBe(0);
  });

  it('emits cancelled on backdrop click', () => {
    const el = open();
    (el.querySelector('.modal-backdrop') as HTMLButtonElement).click();

    expect(cancelledCount).toBe(1);
  });

  it('moves focus into the modal on open and back to the trigger on close', () => {
    const trigger = document.createElement('button');
    document.body.appendChild(trigger);
    trigger.focus();

    const el = open();
    expect(document.activeElement).toBe(el.querySelector('.modal__foot .btn:not(.btn--primary)'));

    fixture.componentRef.setInput('open', false);
    fixture.detectChanges();
    expect(document.activeElement).toBe(trigger);

    trigger.remove();
  });

  it('emits cancelled on Escape only while open', () => {
    fixture.detectChanges();
    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape'}));
    expect(cancelledCount).toBe(0);

    open();
    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape'}));
    expect(cancelledCount).toBe(1);
  });
});

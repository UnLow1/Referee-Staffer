import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DrawerComponent} from './drawer.component';

describe('DrawerComponent', () => {
  let fixture: ComponentFixture<DrawerComponent>;
  let component: DrawerComponent;
  let closedCount: number;

  beforeEach(async () => {
    await TestBed.configureTestingModule({imports: [DrawerComponent]}).compileComponents();
    fixture = TestBed.createComponent(DrawerComponent);
    component = fixture.componentInstance;
    closedCount = 0;
    component.closed.subscribe(() => closedCount++);
  });

  function open(): void {
    fixture.componentRef.setInput('open', true);
    fixture.componentRef.setInput('title', 'Inspect match');
    fixture.detectChanges();
  }

  function pressEscape(): void {
    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape'}));
  }

  it('renders nothing while closed', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.drawer')).toBeNull();
  });

  it('renders the panel with title and optional subtitle when open', () => {
    fixture.componentRef.setInput('subtitle', 'Queue 3');
    open();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.drawer__title')?.textContent).toContain('Inspect match');
    expect(el.querySelector('.fdrawer__sub')?.textContent).toContain('Queue 3');
  });

  it('emits closed on the X button', () => {
    open();
    (fixture.nativeElement.querySelector('button[aria-label="Close"]') as HTMLButtonElement).click();
    expect(closedCount).toBe(1);
  });

  it('emits closed on backdrop click', () => {
    open();
    (fixture.nativeElement.querySelector('.drawer-backdrop') as HTMLButtonElement).click();
    expect(closedCount).toBe(1);
  });

  it('emits closed on Escape only while open', () => {
    fixture.detectChanges();
    pressEscape();
    expect(closedCount).toBe(0);

    open();
    pressEscape();
    expect(closedCount).toBe(1);
  });

  it('applies the wide variant class', () => {
    fixture.componentRef.setInput('wide', true);
    open();
    expect(fixture.nativeElement.querySelector('.drawer--wide')).not.toBeNull();
  });

  it('moves focus into the panel on open and back to the trigger on close', () => {
    const trigger = document.createElement('button');
    document.body.appendChild(trigger);
    trigger.focus();

    open();
    expect(document.activeElement).toBe(fixture.nativeElement.querySelector('button[aria-label="Close"]'));

    fixture.componentRef.setInput('open', false);
    fixture.detectChanges();
    expect(document.activeElement).toBe(trigger);

    trigger.remove();
  });
});

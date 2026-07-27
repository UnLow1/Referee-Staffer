import {ComponentFixture, TestBed} from '@angular/core/testing';
import {PaginatorComponent} from './paginator.component';

describe('PaginatorComponent', () => {
  let fixture: ComponentFixture<PaginatorComponent>;
  let component: PaginatorComponent;
  let emittedPages: number[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({imports: [PaginatorComponent]}).compileComponents();
    fixture = TestBed.createComponent(PaginatorComponent);
    component = fixture.componentInstance;
    emittedPages = [];
    component.pageChange.subscribe(p => emittedPages.push(p));
  });

  function render(total: number, page: number, pageSize = 25): HTMLElement {
    fixture.componentRef.setInput('total', total);
    fixture.componentRef.setInput('page', page);
    fixture.componentRef.setInput('pageSize', pageSize);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  function buttons(el: HTMLElement): {prev: HTMLButtonElement, next: HTMLButtonElement} {
    const [prev, next] = Array.from(el.querySelectorAll<HTMLButtonElement>('.pager__controls .btn'));
    return {prev, next};
  }

  it('shows the visible range and page count', () => {
    const el = render(240, 2);

    expect(el.querySelector('.pager__range')?.textContent).toBe('26–50 of 240');
    expect(el.querySelector('.pager__page')?.textContent?.trim()).toBe('2 / 10');
  });

  it('caps the range end on the last, partial page', () => {
    const el = render(53, 3);

    expect(el.querySelector('.pager__range')?.textContent).toBe('51–53 of 53');
  });

  it('shows an empty range for zero items', () => {
    const el = render(0, 1);

    expect(el.querySelector('.pager__range')?.textContent).toBe('0–0 of 0');
    expect(el.querySelector('.pager__page')?.textContent?.trim()).toBe('1 / 1');
  });

  it('disables prev on the first page and next on the last', () => {
    let el = render(60, 1);
    expect(buttons(el).prev.disabled).toBeTrue();
    expect(buttons(el).next.disabled).toBeFalse();

    el = render(60, 3);
    expect(buttons(el).prev.disabled).toBeFalse();
    expect(buttons(el).next.disabled).toBeTrue();
  });

  it('emits the neighbouring page on prev/next clicks', () => {
    const el = render(60, 2);

    buttons(el).next.click();
    buttons(el).prev.click();

    expect(emittedPages).toEqual([3, 1]);
  });

  it('never emits a page outside 1..totalPages', () => {
    component.total = 60;
    component.pageSize = 25;

    component.goTo(0);
    component.goTo(4);

    expect(emittedPages).toEqual([]);
  });
});

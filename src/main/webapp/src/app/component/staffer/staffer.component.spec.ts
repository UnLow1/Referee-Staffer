import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { StafferComponent } from './staffer.component';

describe('StafferComponent', () => {
  let component: StafferComponent;
  let fixture: ComponentFixture<StafferComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ StafferComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(StafferComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { StafferComponent } from './staffer.component';

describe('StafferComponent', () => {
  let component: StafferComponent;
  let fixture: ComponentFixture<StafferComponent>;

  beforeEach(waitForAsync(() => {
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

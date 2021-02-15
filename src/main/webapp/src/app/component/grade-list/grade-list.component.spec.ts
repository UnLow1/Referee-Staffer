import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { GradeListComponent } from './grade-list.component';

describe('GradeListComponent', () => {
  let component: GradeListComponent;
  let fixture: ComponentFixture<GradeListComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ GradeListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(GradeListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

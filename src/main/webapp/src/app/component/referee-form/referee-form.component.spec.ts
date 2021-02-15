import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { RefereeFormComponent } from './referee-form.component';

describe('RefereeFormComponent', () => {
  let component: RefereeFormComponent;
  let fixture: ComponentFixture<RefereeFormComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ RefereeFormComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RefereeFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

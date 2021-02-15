import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { RefereeListComponent } from './referee-list.component';

describe('RefereeListComponent', () => {
  let component: RefereeListComponent;
  let fixture: ComponentFixture<RefereeListComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ RefereeListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RefereeListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

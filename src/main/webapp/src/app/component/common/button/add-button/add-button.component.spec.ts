import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AddButtonComponent } from './add-button.component';

describe('AddButtonComponent', () => {
  let component: AddButtonComponent;
  let fixture: ComponentFixture<AddButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddButtonComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AddButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

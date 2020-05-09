import { TestBed } from '@angular/core/testing';

import { StafferService } from './staffer.service';

describe('StafferService', () => {
  let service: StafferService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(StafferService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

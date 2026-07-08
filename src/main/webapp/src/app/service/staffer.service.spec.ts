import {TestBed} from '@angular/core/testing';
import {HttpErrorResponse, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';

import {StafferService} from './staffer.service';
import {ToastService} from './toast.service';
import {httpErrorInterceptor} from './http-error.interceptor';
import {Match} from '../model/match';

describe('StafferService', () => {
  const stafferUrl = '/api/staffer';
  const match: Match = {
    id: 3,
    queue: 5,
    homeTeamId: 1,
    awayTeamId: 2,
    date: new Date('2026-05-10T17:00:00Z'),
    refereeId: 7,
    gradeId: 4,
    homeScore: 2,
    awayScore: 1,
    hardnessLvl: 55,
  };

  let service: StafferService;
  let httpTesting: HttpTestingController;
  let toastService: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(StafferService);
    httpTesting = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('staffReferees POSTs to the queue URL with an empty body', () => {
    let result: Match[] | undefined;
    service.staffReferees(5).subscribe(matches => result = matches);

    const req = httpTesting.expectOne(`${stafferUrl}/5`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush([match]);

    expect(result).toEqual([match]);
  });

  it('surfaces a staffing conflict (ProblemDetail) as an error toast and rethrows', () => {
    let error: HttpErrorResponse | undefined;
    service.staffReferees(5).subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(`${stafferUrl}/5`)
      .flush({detail: 'Not enough referees available for queue 5'}, {status: 409, statusText: 'Conflict'});

    expect(error?.status).toBe(409);
    expect(toastService.toast()).toEqual({message: 'Not enough referees available for queue 5', kind: 'error'});
  });

  it('surfaces a 500 without ProblemDetail as a generic error toast and rethrows', () => {
    let error: HttpErrorResponse | undefined;
    service.staffReferees(5).subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(`${stafferUrl}/5`)
      .flush(null, {status: 500, statusText: 'Internal Server Error'});

    expect(error?.status).toBe(500);
    expect(toastService.toast()).toEqual({message: 'Request failed (HTTP 500)', kind: 'error'});
  });
});

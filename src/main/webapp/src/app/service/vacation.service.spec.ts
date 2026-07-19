import {TestBed} from '@angular/core/testing';
import {HttpErrorResponse, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';

import {VacationService} from './vacation.service';
import {ToastService} from './toast.service';
import {httpErrorInterceptor} from './http-error.interceptor';
import {Vacation} from '../model/vacation';

describe('VacationService', () => {
  const vacationsUrl = '/api/vacations';
  const vacation: Vacation = {
    id: 2,
    refereeId: 7,
    startDate: new Date('2026-07-01'),
    endDate: new Date('2026-07-14'),
  };

  let service: VacationService;
  let httpTesting: HttpTestingController;
  let toastService: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(VacationService);
    httpTesting = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('findAll GETs all vacations', () => {
    let result: Vacation[] | undefined;
    service.findAll().subscribe(vacations => result = vacations);

    const req = httpTesting.expectOne(vacationsUrl);
    expect(req.request.method).toBe('GET');
    req.flush([vacation]);

    expect(result).toEqual([vacation]);
  });

  it('findById GETs a single vacation by id', () => {
    let result: Vacation | undefined;
    service.findById(2).subscribe(v => result = v);

    const req = httpTesting.expectOne(`${vacationsUrl}/2`);
    expect(req.request.method).toBe('GET');
    req.flush(vacation);

    expect(result).toEqual(vacation);
  });

  it('save POSTs the new vacation', () => {
    let result: Vacation | undefined;
    service.save(vacation).subscribe(v => result = v);

    const req = httpTesting.expectOne(vacationsUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(vacation);
    req.flush(vacation);

    expect(result).toEqual(vacation);
  });

  it('update PUTs the vacation to the collection URL', () => {
    let result: Vacation | undefined;
    service.update(vacation).subscribe(v => result = v);

    const req = httpTesting.expectOne(vacationsUrl);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(vacation);
    req.flush(vacation);

    expect(result).toEqual(vacation);
  });

  it('delete DELETEs the vacation by id', () => {
    let completed = false;
    service.delete(2).subscribe({complete: () => completed = true});

    const req = httpTesting.expectOne(`${vacationsUrl}/2`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(completed).toBeTrue();
  });

  it('surfaces a 404 as an error toast with the ProblemDetail message and rethrows', () => {
    let error: HttpErrorResponse | undefined;
    service.findById(99).subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(`${vacationsUrl}/99`)
      .flush({detail: 'Vacation with id 99 not found'}, {status: 404, statusText: 'Not Found'});

    expect(error?.status).toBe(404);
    expect(toastService.toast()).toEqual({message: 'Vacation with id 99 not found', kind: 'error'});
  });

  it('surfaces a 500 without ProblemDetail as a generic error toast and rethrows', () => {
    let error: HttpErrorResponse | undefined;
    service.findAll().subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(vacationsUrl)
      .flush(null, {status: 500, statusText: 'Internal Server Error'});

    expect(error?.status).toBe(500);
    expect(toastService.toast()).toEqual({message: 'Request failed (HTTP 500)', kind: 'error'});
  });
});

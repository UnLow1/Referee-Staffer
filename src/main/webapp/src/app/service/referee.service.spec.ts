import {TestBed} from '@angular/core/testing';
import {HttpErrorResponse, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';

import {RefereeService} from './referee.service';
import {ToastService} from './toast.service';
import {httpErrorInterceptor} from './http-error.interceptor';
import {Referee} from '../model/referee';

describe('RefereeService', () => {
  const refereesUrl = '/api/referees';
  const referee: Referee = {
    id: 7,
    firstName: 'Jan',
    lastName: 'Kowalski',
    email: 'jan.kowalski@example.com',
    experience: 5,
    averageGrade: 8.1,
    lastQueue: 12,
  };

  let service: RefereeService;
  let httpTesting: HttpTestingController;
  let toastService: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(RefereeService);
    httpTesting = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('findAll GETs all referees', () => {
    let result: Referee[] | undefined;
    service.findAll().subscribe(referees => result = referees);

    const req = httpTesting.expectOne(refereesUrl);
    expect(req.request.method).toBe('GET');
    req.flush([referee]);

    expect(result).toEqual([referee]);
  });

  it('findById GETs a single referee by id', () => {
    let result: Referee | undefined;
    service.findById(7).subscribe(r => result = r);

    const req = httpTesting.expectOne(`${refereesUrl}/7`);
    expect(req.request.method).toBe('GET');
    req.flush(referee);

    expect(result).toEqual(referee);
  });

  it('save POSTs the new referee', () => {
    let result: Referee | undefined;
    service.save(referee).subscribe(r => result = r);

    const req = httpTesting.expectOne(refereesUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(referee);
    req.flush(referee);

    expect(result).toEqual(referee);
  });

  it('update PUTs the referee to the collection URL', () => {
    let result: Referee | undefined;
    service.update(referee).subscribe(r => result = r);

    const req = httpTesting.expectOne(refereesUrl);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(referee);
    req.flush(referee);

    expect(result).toEqual(referee);
  });

  it('findByIds POSTs the ids wrapped in an object', () => {
    let result: Referee[] | undefined;
    service.findByIds([7, 8]).subscribe(referees => result = referees);

    const req = httpTesting.expectOne(`${refereesUrl}/byIds`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ids: [7, 8]});
    expect(req.request.headers.get('Content-Type')).toBe('application/json');
    req.flush([referee]);

    expect(result).toEqual([referee]);
  });

  it('findRefereesAvailableForQueue GETs the availability endpoint for the queue', () => {
    let result: Referee[] | undefined;
    service.findRefereesAvailableForQueue(12).subscribe(referees => result = referees);

    const req = httpTesting.expectOne(`${refereesUrl}/available/12`);
    expect(req.request.method).toBe('GET');
    req.flush([referee]);

    expect(result).toEqual([referee]);
  });

  it('delete DELETEs the referee by id', () => {
    let completed = false;
    service.delete(7).subscribe({complete: () => completed = true});

    const req = httpTesting.expectOne(`${refereesUrl}/7`);
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

    httpTesting.expectOne(`${refereesUrl}/99`)
      .flush({detail: 'Referee with id 99 not found'}, {status: 404, statusText: 'Not Found'});

    expect(error?.status).toBe(404);
    expect(toastService.toast()).toEqual({message: 'Referee with id 99 not found', kind: 'error'});
  });

  it('surfaces a 500 without ProblemDetail as a generic error toast and rethrows', () => {
    let error: HttpErrorResponse | undefined;
    service.findAll().subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(refereesUrl)
      .flush(null, {status: 500, statusText: 'Internal Server Error'});

    expect(error?.status).toBe(500);
    expect(toastService.toast()).toEqual({message: 'Request failed (HTTP 500)', kind: 'error'});
  });
});

import {TestBed} from '@angular/core/testing';
import {HttpErrorResponse, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';

import {GradeService} from './grade.service';
import {ToastService} from './toast.service';
import {httpErrorInterceptor} from './http-error.interceptor';
import {Grade} from '../model/grade';
import {Match} from '../model/match';

describe('GradeService', () => {
  const gradesUrl = '/api/grades';
  const grade: Grade = {id: 4, value: 8.3};
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
  };

  let service: GradeService;
  let httpTesting: HttpTestingController;
  let toastService: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(GradeService);
    httpTesting = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('findAll GETs all grades', () => {
    let result: Grade[] | undefined;
    service.findAll().subscribe(grades => result = grades);

    const req = httpTesting.expectOne(gradesUrl);
    expect(req.request.method).toBe('GET');
    req.flush([grade]);

    expect(result).toEqual([grade]);
  });

  it('findById GETs a single grade by id', () => {
    let result: Grade | undefined;
    service.findById(4).subscribe(g => result = g);

    const req = httpTesting.expectOne(`${gradesUrl}/4`);
    expect(req.request.method).toBe('GET');
    req.flush(grade);

    expect(result).toEqual(grade);
  });

  it('findByIds POSTs the ids wrapped in an object', () => {
    let result: Grade[] | undefined;
    service.findByIds([4, 5]).subscribe(grades => result = grades);

    const req = httpTesting.expectOne(`${gradesUrl}/byIds`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ids: [4, 5]});
    expect(req.request.headers.get('Content-Type')).toBe('application/json');
    req.flush([grade]);

    expect(result).toEqual([grade]);
  });

  it('save POSTs the grade to the match id URL', () => {
    let result: Grade | undefined;
    service.save(match, grade).subscribe(g => result = g);

    const req = httpTesting.expectOne(`${gradesUrl}/3`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(grade);
    req.flush(grade);

    expect(result).toEqual(grade);
  });

  it('update PUTs the grade to the collection URL', () => {
    let result: Grade | undefined;
    service.update(grade).subscribe(g => result = g);

    const req = httpTesting.expectOne(gradesUrl);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(grade);
    req.flush(grade);

    expect(result).toEqual(grade);
  });

  it('delete DELETEs the grade by its id', () => {
    let completed = false;
    service.delete(grade).subscribe({complete: () => completed = true});

    const req = httpTesting.expectOne(`${gradesUrl}/4`);
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

    httpTesting.expectOne(`${gradesUrl}/99`)
      .flush({detail: 'Grade with id 99 not found'}, {status: 404, statusText: 'Not Found'});

    expect(error?.status).toBe(404);
    expect(toastService.toast()).toEqual({message: 'Grade with id 99 not found', kind: 'error'});
  });

  it('surfaces a 500 without ProblemDetail as a generic error toast and rethrows', () => {
    let error: HttpErrorResponse | undefined;
    service.findAll().subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(gradesUrl)
      .flush(null, {status: 500, statusText: 'Internal Server Error'});

    expect(error?.status).toBe(500);
    expect(toastService.toast()).toEqual({message: 'Request failed (HTTP 500)', kind: 'error'});
  });
});

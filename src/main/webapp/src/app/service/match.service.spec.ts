import {TestBed} from '@angular/core/testing';
import {HttpErrorResponse, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';

import {MatchService} from './match.service';
import {ToastService} from './toast.service';
import {httpErrorInterceptor} from './http-error.interceptor';
import {Match} from '../model/match';
import {DifficultyBreakdown} from '../model/difficultyBreakdown';

describe('MatchService', () => {
  const matchesUrl = '/api/matches';
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

  let service: MatchService;
  let httpTesting: HttpTestingController;
  let toastService: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(MatchService);
    httpTesting = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('findAll GETs all matches', () => {
    let result: Match[] | undefined;
    service.findAll().subscribe(matches => result = matches);

    const req = httpTesting.expectOne(matchesUrl);
    expect(req.request.method).toBe('GET');
    req.flush([match]);

    expect(result).toEqual([match]);
  });

  it('findById GETs a single match by id', () => {
    let result: Match | undefined;
    service.findById(3).subscribe(m => result = m);

    const req = httpTesting.expectOne(`${matchesUrl}/3`);
    expect(req.request.method).toBe('GET');
    req.flush(match);

    expect(result).toEqual(match);
  });

  it('save POSTs the new match', () => {
    let result: Match | undefined;
    service.save(match).subscribe(m => result = m);

    const req = httpTesting.expectOne(matchesUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(match);
    req.flush(match);

    expect(result).toEqual(match);
  });

  it('update PUTs the match to its id URL', () => {
    let result: Match | undefined;
    service.update(match).subscribe(m => result = m);

    const req = httpTesting.expectOne(`${matchesUrl}/3`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(match);
    req.flush(match);

    expect(result).toEqual(match);
  });

  it('updateList PUTs the whole list to the collection URL', () => {
    let completed = false;
    service.updateList([match]).subscribe({complete: () => completed = true});

    const req = httpTesting.expectOne(matchesUrl);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual([match]);
    req.flush(null);

    expect(completed).toBeTrue();
  });

  it('delete DELETEs the match by id', () => {
    let completed = false;
    service.delete(3).subscribe({complete: () => completed = true});

    const req = httpTesting.expectOne(`${matchesUrl}/3`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(completed).toBeTrue();
  });

  it('getDifficultyBreakdown GETs the difficulty endpoint for the match', () => {
    const breakdown: DifficultyBreakdown = {
      matchId: 3,
      total: 55,
      parts: {base: 40, sameCity: 15, top: 0, bottom: 0},
      flags: {sameCity: true, isTop: false, isBot: false, pointsDiff: 4},
    };
    let result: DifficultyBreakdown | undefined;
    service.getDifficultyBreakdown(3).subscribe(b => result = b);

    const req = httpTesting.expectOne(`${matchesUrl}/3/difficulty`);
    expect(req.request.method).toBe('GET');
    req.flush(breakdown);

    expect(result).toEqual(breakdown);
  });

  it('surfaces a 404 as an error toast with the ProblemDetail message and rethrows', () => {
    let error: HttpErrorResponse | undefined;
    service.findById(99).subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(`${matchesUrl}/99`)
      .flush({detail: 'Match with id 99 not found'}, {status: 404, statusText: 'Not Found'});

    expect(error?.status).toBe(404);
    expect(toastService.toast()).toEqual({message: 'Match with id 99 not found', kind: 'error'});
  });

  it('surfaces a 500 without ProblemDetail as a generic error toast and rethrows', () => {
    let error: HttpErrorResponse | undefined;
    service.findAll().subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(matchesUrl)
      .flush(null, {status: 500, statusText: 'Internal Server Error'});

    expect(error?.status).toBe(500);
    expect(toastService.toast()).toEqual({message: 'Request failed (HTTP 500)', kind: 'error'});
  });
});

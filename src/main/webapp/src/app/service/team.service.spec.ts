import {TestBed} from '@angular/core/testing';
import {HttpErrorResponse, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';

import {TeamService} from './team.service';
import {ToastService} from './toast.service';
import {httpErrorInterceptor} from './http-error.interceptor';
import {Team} from '../model/team';

describe('TeamService', () => {
  const teamsUrl = '/api/teams';
  const team: Team = {id: 1, name: 'Legia', city: 'Warszawa', points: 42, short: 'LEG'};

  let service: TeamService;
  let httpTesting: HttpTestingController;
  let toastService: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        // Same interceptor chain as in main.ts, so error specs exercise the real
        // toast-and-rethrow behaviour, not a bare HttpClient.
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(TeamService);
    httpTesting = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('findAll GETs all teams', () => {
    let result: Team[] | undefined;
    service.findAll().subscribe(teams => result = teams);

    const req = httpTesting.expectOne(teamsUrl);
    expect(req.request.method).toBe('GET');
    req.flush([team]);

    expect(result).toEqual([team]);
  });

  it('findById GETs a single team by id', () => {
    let result: Team | undefined;
    service.findById(1).subscribe(t => result = t);

    const req = httpTesting.expectOne(`${teamsUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(team);

    expect(result).toEqual(team);
  });

  it('findByIds POSTs the ids wrapped in an object', () => {
    let result: Team[] | undefined;
    service.findByIds([1, 2]).subscribe(teams => result = teams);

    const req = httpTesting.expectOne(`${teamsUrl}/byIds`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ids: [1, 2]});
    expect(req.request.headers.get('Content-Type')).toBe('application/json');
    req.flush([team]);

    expect(result).toEqual([team]);
  });

  it('save POSTs the new team', () => {
    let result: Team | undefined;
    service.save(team).subscribe(t => result = t);

    const req = httpTesting.expectOne(teamsUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(team);
    req.flush(team);

    expect(result).toEqual(team);
  });

  it('update PUTs the team to the collection URL', () => {
    let result: Team | undefined;
    service.update(team).subscribe(t => result = t);

    const req = httpTesting.expectOne(teamsUrl);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(team);
    req.flush(team);

    expect(result).toEqual(team);
  });

  it('getStandings GETs the standings endpoint', () => {
    let result: Team[] | undefined;
    service.getStandings().subscribe(teams => result = teams);

    const req = httpTesting.expectOne(`${teamsUrl}/standings`);
    expect(req.request.method).toBe('GET');
    req.flush([team]);

    expect(result).toEqual([team]);
  });

  it('delete DELETEs the team by id', () => {
    let completed = false;
    service.delete(1).subscribe({complete: () => completed = true});

    const req = httpTesting.expectOne(`${teamsUrl}/1`);
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

    httpTesting.expectOne(`${teamsUrl}/99`)
      .flush({detail: 'Team with id 99 not found'}, {status: 404, statusText: 'Not Found'});

    expect(error?.status).toBe(404);
    expect(toastService.toast()).toEqual({message: 'Team with id 99 not found', kind: 'error'});
  });

  it('surfaces a 500 without ProblemDetail as a generic error toast and rethrows', () => {
    let error: HttpErrorResponse | undefined;
    service.findAll().subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(teamsUrl)
      .flush(null, {status: 500, statusText: 'Internal Server Error'});

    expect(error?.status).toBe(500);
    expect(toastService.toast()).toEqual({message: 'Request failed (HTTP 500)', kind: 'error'});
  });
});

import {TestBed} from '@angular/core/testing';
import {HttpErrorResponse, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';

import {ConfigurationService} from './configuration.service';
import {ToastService} from './toast.service';
import {httpErrorInterceptor} from './http-error.interceptor';
import {Config} from '../model/config';

describe('ConfigurationService', () => {
  const configurationUrl = '/api/configuration';
  const configs: Config[] = [
    {id: 1, name: 'AVERAGE_GRADE_MULTIPLIER', value: 10, group: 'potential'},
    {id: 2, name: 'NUMBER_OF_EDGE_TEAMS', value: 3, group: 'difficulty'},
  ];

  let service: ConfigurationService;
  let httpTesting: HttpTestingController;
  let toastService: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ConfigurationService);
    httpTesting = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('findAll GETs all config entries', () => {
    let result: Config[] | undefined;
    service.findAll().subscribe(c => result = c);

    const req = httpTesting.expectOne(configurationUrl);
    expect(req.request.method).toBe('GET');
    req.flush(configs);

    expect(result).toEqual(configs);
  });

  it('update PUTs the whole config list', () => {
    let result: Config[] | undefined;
    service.update(configs).subscribe(c => result = c);

    const req = httpTesting.expectOne(configurationUrl);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(configs);
    req.flush(configs);

    expect(result).toEqual(configs);
  });

  it('surfaces a 404 as an error toast with the ProblemDetail message and rethrows', () => {
    let error: HttpErrorResponse | undefined;
    service.update(configs).subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(configurationUrl)
      .flush({detail: 'Config AVERAGE_GRADE_MULTIPLIER not found'}, {status: 404, statusText: 'Not Found'});

    expect(error?.status).toBe(404);
    expect(toastService.toast()).toEqual({message: 'Config AVERAGE_GRADE_MULTIPLIER not found', kind: 'error'});
  });

  it('surfaces a 500 without ProblemDetail as a generic error toast and rethrows', () => {
    let error: HttpErrorResponse | undefined;
    service.findAll().subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(configurationUrl)
      .flush(null, {status: 500, statusText: 'Internal Server Error'});

    expect(error?.status).toBe(500);
    expect(toastService.toast()).toEqual({message: 'Request failed (HTTP 500)', kind: 'error'});
  });
});

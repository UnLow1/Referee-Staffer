import {TestBed} from '@angular/core/testing';
import {HttpErrorResponse, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {firstValueFrom} from 'rxjs';

import {ImporterService} from './importer.service';
import {ToastService} from './toast.service';
import {httpErrorInterceptor} from './http-error.interceptor';
import {ImportResponse} from '../request/importResponse';

describe('ImporterService', () => {
  const importerUrl = '/api/importer';

  let service: ImporterService;
  let httpTesting: HttpTestingController;
  let toastService: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ImporterService);
    httpTesting = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('postFile POSTs the file and queue count as multipart form data', () => {
    const file = new File(['queue;home;away'], 'season.csv', {type: 'text/csv'});
    const response: ImportResponse = {matches: 240, referees: 20, grades: 180, teams: 16};
    let result: ImportResponse | undefined;
    service.postFile(file, 30).subscribe(r => result = r);

    const req = httpTesting.expectOne(importerUrl);
    expect(req.request.method).toBe('POST');
    const body = req.request.body as FormData;
    expect(body instanceof FormData).toBeTrue();
    expect((body.get('file') as File).name).toBe('season.csv');
    expect(body.get('numberOfQueuesToImport')).toBe('30');
    req.flush(response);

    expect(result).toEqual(response);
  });

  it('downloadExampleFile GETs the example endpoint as a blob', () => {
    const blob = new Blob(['queue;home;away'], {type: 'text/csv'});
    let result: Blob | undefined;
    service.downloadExampleFile().subscribe(b => result = b);

    const req = httpTesting.expectOne(`${importerUrl}/example`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(blob);

    expect(result).toBe(blob);
  });

  it('surfaces an import failure (ProblemDetail) as an error toast and rethrows', () => {
    const file = new File(['broken'], 'broken.csv', {type: 'text/csv'});
    let error: HttpErrorResponse | undefined;
    service.postFile(file, 30).subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(importerUrl)
      .flush({detail: 'Import failed: invalid CSV header'}, {status: 400, statusText: 'Bad Request'});

    expect(error?.status).toBe(400);
    expect(toastService.toast()).toEqual({message: 'Import failed: invalid CSV header', kind: 'error'});
  });

  it('extracts the ProblemDetail message from a JSON-typed Blob error body', async () => {
    // Blob download errors carry the body as a Blob — the interceptor reads it
    // asynchronously, so the error is awaited instead of asserted synchronously.
    const errorPromise = firstValueFrom(service.downloadExampleFile())
      .then(() => fail('expected an error'), (e: HttpErrorResponse) => e);

    const problemDetail = new Blob(
      [JSON.stringify({detail: 'Example file is missing'})],
      {type: 'application/problem+json'},
    );
    httpTesting.expectOne(`${importerUrl}/example`)
      .flush(problemDetail, {status: 404, statusText: 'Not Found'});

    const error = await errorPromise as HttpErrorResponse;
    expect(error.status).toBe(404);
    expect(toastService.toast()).toEqual({message: 'Example file is missing', kind: 'error'});
  });

  it('surfaces a 500 without ProblemDetail as a generic error toast and rethrows', () => {
    let error: HttpErrorResponse | undefined;
    service.downloadExampleFile().subscribe({
      next: () => fail('expected an error'),
      error: (e: HttpErrorResponse) => error = e,
    });

    httpTesting.expectOne(`${importerUrl}/example`)
      .flush(new Blob(), {status: 500, statusText: 'Internal Server Error'});

    expect(error?.status).toBe(500);
    expect(toastService.toast()).toEqual({message: 'Request failed (HTTP 500)', kind: 'error'});
  });
});

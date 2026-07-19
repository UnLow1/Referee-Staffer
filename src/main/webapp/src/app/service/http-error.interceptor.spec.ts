import {TestBed} from '@angular/core/testing';
import {HttpClient, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {httpErrorInterceptor} from './http-error.interceptor';
import {ToastService} from './toast.service';

describe('httpErrorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let toastService: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    toastService = jasmine.createSpyObj('ToastService', ['show']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
        {provide: ToastService, useValue: toastService}
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // Blob.text() resolves outside Angular's zone, so poll briefly instead of a fixed sleep.
  async function waitForToast(): Promise<void> {
    for (let i = 0; i < 50 && toastService.show.calls.count() === 0; i++) {
      await new Promise(resolve => setTimeout(resolve, 1));
    }
  }

  it('shows the ProblemDetail detail for JSON error responses', () => {
    http.get('/api/things/1').subscribe({error: () => undefined});

    httpMock.expectOne('/api/things/1')
      .flush({detail: 'Match with id = 1 has not been found'}, {status: 404, statusText: 'Not Found'});

    expect(toastService.show).toHaveBeenCalledWith('Match with id = 1 has not been found', 'error');
  });

  it('shows a connectivity message when the server is unreachable', () => {
    http.get('/api/things').subscribe({error: () => undefined});

    httpMock.expectOne('/api/things').error(new ProgressEvent('error'), {status: 0});

    expect(toastService.show).toHaveBeenCalledWith('Cannot reach the server', 'error');
  });

  it('falls back to a generic message when there is no detail', () => {
    http.get('/api/things').subscribe({error: () => undefined});

    httpMock.expectOne('/api/things').flush('boom', {status: 500, statusText: 'Server Error'});

    expect(toastService.show).toHaveBeenCalledWith('Request failed (HTTP 500)', 'error');
  });

  it('reads the ProblemDetail detail out of blob error responses', async () => {
    http.get('/api/matches/queue/44/pdf', {responseType: 'blob'}).subscribe({error: () => undefined});
    const problemJson = new Blob(
      [JSON.stringify({detail: 'No matches have been found for queue = 44'})],
      {type: 'application/problem+json'});

    httpMock.expectOne('/api/matches/queue/44/pdf')
      .flush(problemJson, {status: 404, statusText: 'Not Found'});
    await waitForToast();

    expect(toastService.show).toHaveBeenCalledWith('No matches have been found for queue = 44', 'error');
  });

  it('falls back to a generic message for a JSON-typed blob that is not valid JSON', async () => {
    http.get('/api/matches/queue/44/pdf', {responseType: 'blob'}).subscribe({error: () => undefined});
    const brokenBlob = new Blob(['not json'], {type: 'application/problem+json'});

    httpMock.expectOne('/api/matches/queue/44/pdf')
      .flush(brokenBlob, {status: 404, statusText: 'Not Found'});
    await waitForToast();

    expect(toastService.show).toHaveBeenCalledWith('Request failed (HTTP 404)', 'error');
  });

  it('rethrows the error so component callbacks still run', () => {
    let caught: unknown;
    http.get('/api/things').subscribe({error: err => caught = err});

    httpMock.expectOne('/api/things').flush(null, {status: 500, statusText: 'Server Error'});

    expect(caught).toBeDefined();
  });
});

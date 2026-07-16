import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {ConfigurationService, DEFAULT_NUMBER_OF_EDGE_TEAMS} from './configuration.service';
import {Config} from '../model/config';
import {environment} from '../../environments/environment';

describe('ConfigurationService', () => {
  let service: ConfigurationService;
  let httpMock: HttpTestingController;

  const url = `${environment.apiBaseUrl}/api/configuration`;

  function config(name: string, value: number): Config {
    return {id: 1, name, value};
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ConfigurationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('edgeTeams', () => {
    it('starts on the fallback before any fetch', () => {
      expect(service.edgeTeams()).toBe(DEFAULT_NUMBER_OF_EDGE_TEAMS);
    });

    it('loads NUMBER_OF_EDGE_TEAMS once and ignores repeat calls', () => {
      service.ensureEdgeTeamsLoaded();
      httpMock.expectOne(url).flush([config('NUMBER_OF_EDGE_TEAMS', 4.0)]);

      expect(service.edgeTeams()).toBe(4);

      service.ensureEdgeTeamsLoaded();
      httpMock.expectNone(url);
    });

    it('retries on a later call after the fetch fails, keeping the fallback meanwhile', () => {
      service.ensureEdgeTeamsLoaded();
      httpMock.expectOne(url).flush(null, {status: 500, statusText: 'Server Error'});
      expect(service.edgeTeams()).toBe(DEFAULT_NUMBER_OF_EDGE_TEAMS);

      service.ensureEdgeTeamsLoaded();
      httpMock.expectOne(url).flush([config('NUMBER_OF_EDGE_TEAMS', 5.0)]);
      expect(service.edgeTeams()).toBe(5);
    });

    it('keeps the current value when the key is missing or not positive', () => {
      service.ensureEdgeTeamsLoaded();
      httpMock.expectOne(url).flush([config('EXPERIENCE_MULTIPLIER', 0.01)]);
      expect(service.edgeTeams()).toBe(DEFAULT_NUMBER_OF_EDGE_TEAMS);

      service.update([config('NUMBER_OF_EDGE_TEAMS', 0)]).subscribe();
      httpMock.expectOne(url).flush([config('NUMBER_OF_EDGE_TEAMS', 0)]);
      expect(service.edgeTeams()).toBe(DEFAULT_NUMBER_OF_EDGE_TEAMS);
    });

    it('stays in sync when the configuration screen saves a new value', () => {
      const updated = [config('NUMBER_OF_EDGE_TEAMS', 2.0)];

      service.update(updated).subscribe();
      httpMock.expectOne(url).flush(updated);

      expect(service.edgeTeams()).toBe(2);
    });
  });

  describe('findAll', () => {
    it('fetches the configuration list', () => {
      const configs = [config('NUMBER_OF_EDGE_TEAMS', 3.0)];
      let result: Config[] | undefined;

      service.findAll().subscribe(c => result = c);
      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('GET');
      req.flush(configs);

      expect(result).toEqual(configs);
    });
  });

  describe('update', () => {
    it('puts the configuration list', () => {
      const configs = [config('AVERAGE_GRADE_MULTIPLIER', 50)];
      let result: Config[] | undefined;

      service.update(configs).subscribe(c => result = c);
      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(configs);
      req.flush(configs);

      expect(result).toEqual(configs);
    });
  });
});

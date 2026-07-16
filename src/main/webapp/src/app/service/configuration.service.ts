import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from "@angular/common/http";
import {Observable, tap} from "rxjs";
import {Config} from "../model/config";
import {environment} from "../../environments/environment";

/** Fallback until /api/configuration responds (also the seed value in data.sql). */
export const DEFAULT_NUMBER_OF_EDGE_TEAMS = 3;

@Injectable({
  providedIn: 'root'
})
export class ConfigurationService {
  private http = inject(HttpClient);


  private readonly configurationUrl = `${environment.apiBaseUrl}/api/configuration`
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  private readonly edgeTeamsSignal = signal(DEFAULT_NUMBER_OF_EDGE_TEAMS);
  /**
   * NUMBER_OF_EDGE_TEAMS from the backend configuration — the size of the top / bottom
   * edges of the table. Consumers derive zones and flags from this instead of a local
   * hardcode, so the UI follows the configurable server-side value.
   */
  readonly edgeTeams = this.edgeTeamsSignal.asReadonly();
  private edgeTeamsLoaded = false;

  /**
   * One-shot lazy fetch of the configuration to populate {@link edgeTeams}. Idempotent —
   * every screen that derives edge zones calls this on init and only the first call hits
   * the backend. On error the fallback stays and the next call retries.
   */
  public ensureEdgeTeamsLoaded(): void {
    if (this.edgeTeamsLoaded) return;
    this.edgeTeamsLoaded = true;
    this.findAll().subscribe({
      error: () => this.edgeTeamsLoaded = false
    });
  }

  public update(config: Config[]): Observable<Config[]> {
    return this.http.put<Config[]>(this.configurationUrl, config)
      .pipe(tap(configs => this.syncEdgeTeams(configs)))
  }

  public findAll(): Observable<Config[]> {
    return this.http.get<Config[]>(this.configurationUrl)
      .pipe(tap(configs => this.syncEdgeTeams(configs)))
  }

  private syncEdgeTeams(configs: Config[]): void {
    const value = configs.find(c => c.name === 'NUMBER_OF_EDGE_TEAMS')?.value;
    if (value != null && value > 0) {
      this.edgeTeamsSignal.set(Math.round(value));
    }
  }
}

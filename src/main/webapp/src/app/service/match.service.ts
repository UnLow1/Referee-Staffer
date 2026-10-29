import { Injectable, inject } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import {Observable} from "rxjs";
import {Match} from "../model/match";
import {DifficultyBreakdown} from "../model/difficultyBreakdown";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class MatchService {
  private http = inject(HttpClient);


  private readonly matchesUrl = `${environment.apiBaseUrl}/api/matches`

  public findById(id: number): Observable<Match> {
    return this.http.get<Match>(`${this.matchesUrl}/${id}`)
  }

  public findAll(): Observable<Match[]> {
    return this.http.get<Match[]>(this.matchesUrl)
  }

  /** Queues the season actually has, ascending. Bounds the Staffer's queue stepper (RS-115). */
  public getQueues(): Observable<number[]> {
    return this.http.get<number[]>(`${this.matchesUrl}/queues`)
  }

  public save(match: Match): Observable<Match> {
    return this.http.post<Match>(this.matchesUrl, match)
  }

  public update(match: Match): Observable<Match> {
    return this.http.put<Match>(`${this.matchesUrl}/${match.id}`, match)
  }

  public updateList(matches: Match[]): Observable<void> {
    return this.http.put<void>(this.matchesUrl, matches)
  }

  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.matchesUrl}/${id}`)
  }

  public getDifficultyBreakdown(matchId: number): Observable<DifficultyBreakdown> {
    return this.http.get<DifficultyBreakdown>(`${this.matchesUrl}/${matchId}/difficulty`)
  }

  public downloadAssignmentsPdf(queue: number): Observable<Blob> {
    return this.http.get(`${this.matchesUrl}/queue/${queue}/pdf`, {responseType: 'blob'})
  }
}

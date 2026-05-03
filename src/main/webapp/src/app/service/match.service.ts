import { Injectable, inject } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import {Observable} from "rxjs";
import {Match} from "../model/match";
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

  public deleteAll(): Observable<void> {
    return this.http.delete<void>(`${this.matchesUrl}`)
  }
}

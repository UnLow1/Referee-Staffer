import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from "@angular/common/http";
import {Observable} from "rxjs";
import {Team} from "../model/team";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class TeamService {
  private http = inject(HttpClient);


  private readonly teamsUrl = `${environment.apiBaseUrl}/api/teams`
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  public findAll(): Observable<Team[]> {
    return this.http.get<Team[]>(this.teamsUrl)
  }

  public findById(id: number): Observable<Team> {
    return this.http.get<Team>(`${this.teamsUrl}/${id}`, this.httpOptions);
  }

  public findByIds(ids: number[]): Observable<Team[]> {
    return this.http.post<Team[]>(`${this.teamsUrl}/byIds`, {ids}, this.httpOptions);
  }

  public save(team: Team): Observable<Team> {
    return this.http.post<Team>(this.teamsUrl, team)
  }

  public update(team: Team): Observable<Team> {
    return this.http.put<Team>(this.teamsUrl, team)
  }

  public getStandings(): Observable<Team[]> {
    return this.http.get<Team[]>(`${this.teamsUrl}/standings`)
  }

  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.teamsUrl}/${id}`)
  }

  public deleteAll(): Observable<void> {
    return this.http.delete<void>(`${this.teamsUrl}`)
  }
}

import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Observable} from "rxjs";
import {Team} from "../model/team";

@Injectable({
  providedIn: 'root'
})
export class TeamService {

  private readonly teamsUrl: string
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  constructor(private http: HttpClient) {
    this.teamsUrl = 'api/teams'
  }

  public findAll(): Observable<Team[]> {
    return this.http.get<Team[]>(this.teamsUrl)
  }

  public findByIds(ids: number[]): Observable<Team[]> {
    return this.http.post<Team[]>(`${this.teamsUrl}/byIds`, {ids}, this.httpOptions);
  }

  public save(team: Team) {
    return this.http.post<Team>(this.teamsUrl, team)
  }

  public getStandings(): Observable<Team[]> {
    return this.http.get<Team[]>(`${this.teamsUrl}/standings`)
  }
}

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

  public findById(id: number): Observable<Team> {
    return this.http.get<Team>(`${this.teamsUrl}/${id}`, this.httpOptions);
  }

  public findByIds(ids: number[]): Observable<Team[]> {
    return this.http.post<Team[]>(`${this.teamsUrl}/byIds`, {ids}, this.httpOptions);
  }

  public save(team: Team) {
    return this.http.post(this.teamsUrl, team)
  }

  public update(team: Team) {
    return this.http.put(this.teamsUrl, team)
  }

  public getStandings(): Observable<Team[]> {
    return this.http.get<Team[]>(`${this.teamsUrl}/standings`)
  }

  public delete(id: number) {
    return this.http.delete(`${this.teamsUrl}/${id}`)
  }
}

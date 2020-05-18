import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Match} from "../model/match";

@Injectable({
  providedIn: 'root'
})
export class MatchService {

  private readonly matchesUrl: string

  constructor(private http: HttpClient) {
    this.matchesUrl = 'api/matches'
  }

  public findAll(): Observable<Match[]> {
    return this.http.get<Match[]>(this.matchesUrl)
  }

  public save(match: Match): Observable<Match> {
    return this.http.post<Match>(this.matchesUrl, match)
  }

  public update(matches: Match[]) {
    return this.http.put(this.matchesUrl, matches)
  }

  public delete(id: number) {
    return this.http.delete(`${this.matchesUrl}/${id}`)
  }
}

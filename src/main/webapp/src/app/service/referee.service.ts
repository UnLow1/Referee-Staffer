import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Observable} from "rxjs";
import {Referee} from "../model/referee";

@Injectable({
  providedIn: 'root'
})
export class RefereeService {

  private readonly refereesUrl: string
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  constructor(private http: HttpClient) {
    this.refereesUrl = 'api/referees'
  }

  public findAll(): Observable<Referee[]> {
    return this.http.get<Referee[]>(this.refereesUrl)
  }

  public save(referee: Referee) {
    return this.http.post<Referee>(this.refereesUrl, referee)
  }

  public findByIds(ids: number[]): Observable<Referee[]> {
    return this.http.post<Referee[]>(`${this.refereesUrl}/byIds`, {ids}, this.httpOptions);
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from "@angular/common/http";
import {Observable} from "rxjs";
import {Referee} from "../model/referee";

@Injectable({
  providedIn: 'root'
})
export class RefereeService {
  private http = inject(HttpClient);


  private readonly refereesUrl: string
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  constructor() {
    this.refereesUrl = 'api/referees'
  }

  public findAll(): Observable<Referee[]> {
    return this.http.get<Referee[]>(this.refereesUrl)
  }

  public findById(id: number): Observable<Referee> {
    return this.http.get<Referee>(`${this.refereesUrl}/${id}`)
  }

  public update(referee: Referee): Observable<Referee> {
    return this.http.put<Referee>(this.refereesUrl, referee)
  }

  public save(referee: Referee): Observable<Referee> {
    return this.http.post<Referee>(this.refereesUrl, referee)
  }

  public findByIds(ids: number[]): Observable<Referee[]> {
    return this.http.post<Referee[]>(`${this.refereesUrl}/byIds`, {ids}, this.httpOptions);
  }

  public findRefereesAvailableForQueue(queue: number): Observable<Referee[]> {
    return this.http.get<Referee[]>(`${this.refereesUrl}/available/${queue}`, this.httpOptions);
  }

  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.refereesUrl}/${id}`)
  }

  public deleteAll(): Observable<void> {
    return this.http.delete<void>(`${this.refereesUrl}`)
  }
}

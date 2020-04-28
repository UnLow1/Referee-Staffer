import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Referee} from "../model/referee";

@Injectable({
  providedIn: 'root'
})
export class RefereeService {

  private refereesUrl: string;

  constructor(private http: HttpClient) {
    this.refereesUrl = 'http://localhost:8080/referees';
  }

  public findAll(): Observable<Referee[]> {
    return this.http.get<Referee[]>(this.refereesUrl);
  }

  public save(referee: Referee) {
    return this.http.post<Referee>(this.refereesUrl, referee);
  }
}

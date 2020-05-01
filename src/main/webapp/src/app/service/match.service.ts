import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Match} from "../model/match";

@Injectable({
  providedIn: 'root'
})
export class MatchService {

  private matchesUrl: string;

  constructor(private http: HttpClient) {
    this.matchesUrl = 'http://localhost:8080/matches';
  }

  public findAll(): Observable<Match[]> {
    return this.http.get<Match[]>(this.matchesUrl);
  }
}

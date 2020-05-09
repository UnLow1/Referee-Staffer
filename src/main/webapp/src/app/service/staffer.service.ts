import { Injectable } from '@angular/core';
import {Observable} from "rxjs";
import {Referee} from "../model/referee";
import {HttpClient} from "@angular/common/http";
import {Match} from "../model/match";

@Injectable({
  providedIn: 'root'
})
export class StafferService {

  private readonly stafferUrl: string

  constructor(private http: HttpClient) {
    this.stafferUrl = 'http://localhost:8080/staffer'
  }

  public staffReferees(queue: number): Observable<Match[]> {
    return this.http.get<Match[]>(`${this.stafferUrl}/${queue}`)
  }
}

import { Injectable, inject } from '@angular/core';
import {Observable} from "rxjs";
import { HttpClient } from "@angular/common/http";
import {Match} from "../model/match";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class StafferService {
  private http = inject(HttpClient);


  private readonly stafferUrl = `${environment.apiBaseUrl}/api/staffer`

  public staffReferees(queue: number): Observable<Match[]> {
    // POST — staffing mutates assignments server-side (empty body; queue is in the path).
    return this.http.post<Match[]>(`${this.stafferUrl}/${queue}`, null)
  }
}

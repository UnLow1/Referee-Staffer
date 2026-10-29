import { Injectable, inject } from '@angular/core';
import {Observable} from "rxjs";
import { HttpClient } from "@angular/common/http";
import {Match} from "../model/match";
import {StaffingLock} from "../model/staffingLock";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class StafferService {
  private http = inject(HttpClient);


  private readonly stafferUrl = `${environment.apiBaseUrl}/api/staffer`

  /** The cast the backend has stored for a queue — what Save cast last persisted. */
  public getStoredCast(queue: number): Observable<Match[]> {
    return this.http.get<Match[]>(`${this.stafferUrl}/${queue}`)
  }

  public staffReferees(queue: number, locks: StaffingLock[] = []): Observable<Match[]> {
    // POST even though nothing is persisted (RS-105: the response is a draft): the request
    // carries a body of locked (matchId, refereeId) pairs the backend must keep while it
    // re-staffs the rest, and running the algorithm is no cacheable lookup.
    return this.http.post<Match[]>(`${this.stafferUrl}/${queue}`, locks)
  }
}

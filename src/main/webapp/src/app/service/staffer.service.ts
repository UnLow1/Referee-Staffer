import { Injectable, inject } from '@angular/core';
import {Observable} from "rxjs";
import { HttpClient } from "@angular/common/http";
import {Match} from "../model/match";
import {StaffingLock} from "../model/staffingLock";
import {CandidateViolations} from "../model/staffingViolation";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class StafferService {
  private http = inject(HttpClient);


  private readonly stafferUrl = `${environment.apiBaseUrl}/api/staffer`

  public staffReferees(queue: number, locks: StaffingLock[] = []): Observable<Match[]> {
    // POST — staffing mutates assignments server-side (queue is in the path). The body
    // carries locked (matchId, refereeId) pairs the backend must keep while re-staffing.
    return this.http.post<Match[]>(`${this.stafferUrl}/${queue}`, locks)
  }

  /**
   * Staffing rules each (match, referee) pairing in the queue would break. One matrix for the
   * whole queue rather than a request per drawer: the payload only carries conflicting pairs,
   * and the rules depend on data (matches of other queues, vacations) the frontend never loads.
   */
  public findCandidateViolations(queue: number): Observable<CandidateViolations[]> {
    return this.http.get<CandidateViolations[]>(`${this.stafferUrl}/${queue}/violations`)
  }
}

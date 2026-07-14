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

  public staffReferees(queue: number, locks: StaffingLock[] = []): Observable<Match[]> {
    // POST — staffing mutates assignments server-side (queue is in the path). The body
    // carries locked (matchId, refereeId) pairs the backend must keep while re-staffing.
    return this.http.post<Match[]>(`${this.stafferUrl}/${queue}`, locks)
  }
}

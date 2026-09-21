import { Injectable, inject } from '@angular/core';
import {Observable} from "rxjs";
import { HttpClient } from "@angular/common/http";
import {Match} from "../model/match";
import {StaffingLock} from "../model/staffingLock";
import {StaffingOverwrite} from "../model/staffingOverwrite";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class StafferService {
  private http = inject(HttpClient);


  private readonly stafferUrl = `${environment.apiBaseUrl}/api/staffer`

  public getOverwrittenAssignments(queue: number): Observable<StaffingOverwrite> {
    // GET — read-only preview of the assignments a staffing run would clear, used by the
    // Staffer screen's overwrite guard before it posts the (mutating) staffing request.
    return this.http.get<StaffingOverwrite>(`${this.stafferUrl}/${queue}/assignments`)
  }

  public staffReferees(queue: number, locks: StaffingLock[] = []): Observable<Match[]> {
    // POST — staffing mutates assignments server-side (queue is in the path). The body
    // carries locked (matchId, refereeId) pairs the backend must keep while re-staffing.
    return this.http.post<Match[]>(`${this.stafferUrl}/${queue}`, locks)
  }
}

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
    return this.http.get<Match[]>(`${this.stafferUrl}/${queue}`)
  }
}

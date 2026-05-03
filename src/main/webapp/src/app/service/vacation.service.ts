import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from "@angular/common/http";
import {Observable} from "rxjs";
import {Vacation} from "../model/vacation";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class VacationService {
  private http = inject(HttpClient);


  private readonly vacationsUrl = `${environment.apiBaseUrl}/api/vacations`
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  public findAll(): Observable<Vacation[]> {
    return this.http.get<Vacation[]>(this.vacationsUrl)
  }

  public findById(id: number): Observable<Vacation> {
    return this.http.get<Vacation>(`${this.vacationsUrl}/${id}`, this.httpOptions);
  }

  public save(vacation: Vacation): Observable<Vacation> {
    return this.http.post<Vacation>(this.vacationsUrl, vacation)
  }

  public update(vacation: Vacation): Observable<Vacation> {
    return this.http.put<Vacation>(this.vacationsUrl, vacation)
  }

  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.vacationsUrl}/${id}`)
  }

  public deleteAll(): Observable<void> {
    return this.http.delete<void>(`${this.vacationsUrl}`)
  }
}

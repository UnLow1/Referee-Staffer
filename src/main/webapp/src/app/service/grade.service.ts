import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from "@angular/common/http";
import {Observable} from "rxjs";
import {Grade} from "../model/grade";
import {Match} from "../model/match";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class GradeService {
  private http = inject(HttpClient);


  private readonly gradesUrl = `${environment.apiBaseUrl}/api/grades`
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  public update(grade: Grade): Observable<Grade> {
    return this.http.put<Grade>(this.gradesUrl, grade, this.httpOptions)
  }

  public findById(id: number): Observable<Grade> {
    return this.http.get<Grade>(`${this.gradesUrl}/${id}`)
  }

  public findAll(): Observable<Grade[]> {
    return this.http.get<Grade[]>(this.gradesUrl)
  }

  public findByIds(ids: number[]): Observable<Grade[]> {
    return this.http.post<Grade[]>(`${this.gradesUrl}/byIds`, {ids}, this.httpOptions);
  }

  public save(match: Match, grade: Grade): Observable<Grade> {
    return this.http.post<Grade>(`${this.gradesUrl}/${match.id}`, grade)
  }

  public delete(grade: Grade): Observable<void> {
    return this.http.delete<void>(`${this.gradesUrl}/${grade.id}`)
  }

  public deleteAll(): Observable<void> {
    return this.http.delete<void>(`${this.gradesUrl}`)
  }
}

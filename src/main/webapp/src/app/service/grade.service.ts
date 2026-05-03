import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from "@angular/common/http";
import {Observable} from "rxjs";
import {Grade} from "../model/grade";
import {Match} from "../model/match";

@Injectable({
  providedIn: 'root'
})
export class GradeService {
  private http = inject(HttpClient);


  private readonly gradesUrls: string
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  constructor() {
    this.gradesUrls = 'api/grades'
  }

  public update(grade: Grade): Observable<Grade> {
    return this.http.put<Grade>(this.gradesUrls, grade, this.httpOptions)
  }

  public findById(id: number): Observable<Grade> {
    return this.http.get<Grade>(`${this.gradesUrls}/${id}`)
  }

  public findAll(): Observable<Grade[]> {
    return this.http.get<Grade[]>(this.gradesUrls)
  }

  public findByIds(ids: number[]): Observable<Grade[]> {
    return this.http.post<Grade[]>(`${this.gradesUrls}/byIds`, {ids}, this.httpOptions);
  }

  public save(match: Match, grade: Grade): Observable<Grade> {
    return this.http.post<Grade>(`${this.gradesUrls}/${match.id}`, grade)
  }

  public delete(grade: Grade): Observable<void> {
    return this.http.delete<void>(`${this.gradesUrls}/${grade.id}`)
  }

  public deleteAll(): Observable<void> {
    return this.http.delete<void>(`${this.gradesUrls}`)
  }
}

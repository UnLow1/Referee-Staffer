import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Observable} from "rxjs";
import {Grade} from "../model/grade";
import {Match} from "../model/match";

@Injectable({
  providedIn: 'root'
})
export class GradeService {

  private readonly gradesUrls: string
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  constructor(private http: HttpClient) {
    this.gradesUrls = 'http://localhost:8080/grades'
  }

  public findAll(): Observable<Grade[]> {
    return this.http.get<Grade[]>(this.gradesUrls)
  }

  public findByIds(ids: number[]): Observable<Grade[]> {
    return this.http.post<Grade[]>(this.gradesUrls + "/byIds", {ids}, this.httpOptions);
  }

  public save(match: Match, grade: Grade) {
    return this.http.post<Grade>(this.gradesUrls + `/${match.id}`, grade)
  }
}

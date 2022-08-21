import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Observable} from "rxjs";
import {Vacation} from "../model/vacation";

@Injectable({
  providedIn: 'root'
})
export class VacationService {

  private readonly vacationsUrl: string
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  constructor(private http: HttpClient) {
    this.vacationsUrl = 'api/vacations'
  }

  public findAll(): Observable<Vacation[]> {
    return this.http.get<Vacation[]>(this.vacationsUrl)
  }

  public findById(id: number): Observable<Vacation> {
    return this.http.get<Vacation>(`${this.vacationsUrl}/${id}`, this.httpOptions);
  }

  public save(vacation: Vacation) {
    return this.http.post(this.vacationsUrl, vacation)
  }

  public update(vacation: Vacation) {
    return this.http.put(this.vacationsUrl, vacation)
  }

  public delete(id: number) {
    return this.http.delete(`${this.vacationsUrl}/${id}`)
  }

  public deleteAll() {
    return this.http.delete(`${this.vacationsUrl}`)
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from "@angular/common/http";
import {Observable} from "rxjs";
import {Config} from "../model/config";

@Injectable({
  providedIn: 'root'
})
export class ConfigurationService {
  private http = inject(HttpClient);


  private readonly configurationUrl: string = `api/configuration`
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json'
    })
  };

  public update(config: Config[]): Observable<Config[]> {
    return this.http.put<Config[]>(this.configurationUrl, config)
  }

  public findAll(): Observable<Config[]> {
    return this.http.get<Config[]>(this.configurationUrl)
  }
}

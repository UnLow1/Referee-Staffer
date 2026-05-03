import { Injectable, inject } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import {ImportResponse} from "../request/importResponse";
import {Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class ImporterService {
  private http = inject(HttpClient);


  private readonly importerUrl: string

  constructor() {
    this.importerUrl = 'api/importer'
  }

  public postFile(fileToUpload: File, numberOfQueuesToImport: number): Observable<ImportResponse> {
    const formData: FormData = new FormData()
    formData.append('file', fileToUpload, fileToUpload.name)
    formData.append('numberOfQueuesToImport', numberOfQueuesToImport.toString())
    return this.http.post<ImportResponse>(this.importerUrl, formData)
  }

  public downloadExampleFile():Observable<Blob> {
    return this.http.get(`${this.importerUrl}/example`, {
      responseType: 'blob'
    });
  }
}

import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";

@Injectable({
  providedIn: 'root'
})
export class ImporterService {

  private readonly importerUrl: string

  constructor(private http: HttpClient) {
    this.importerUrl = 'http://localhost:8080/importer'
  }

  public postFile(fileToUpload: File) {
    const formData: FormData = new FormData();
    formData.append('file', fileToUpload, fileToUpload.name);
    return this.http.post(this.importerUrl, formData);
  }
}

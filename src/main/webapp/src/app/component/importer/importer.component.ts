import { Component, inject } from '@angular/core';
import {ImporterService} from "../../service/importer.service";
import {ImportResponse} from "../../request/importResponse";
import { saveAs } from 'file-saver';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-importer',
    templateUrl: './importer.component.html',
    styleUrls: ['./importer.component.scss'],
    imports: [FormsModule]
})
export class ImporterComponent {
  private importerService = inject(ImporterService);


  fileToUpload: File | null = null
  importResult: ImportResponse | null = null
  numberOfQueuesToImport: number | null = null

  handleFileInput(event: Event) {
    const input = event.target as HTMLInputElement
    this.fileToUpload = input.files?.item(0) ?? null
  }

  uploadFileToActivity() {
    // Template binds the Save button's `[disabled]` to these being non-null, but the type
    // system can't verify that — guard explicitly so postFile's signature stays strict.
    if (this.fileToUpload === null || this.numberOfQueuesToImport === null) return
    this.importerService.postFile(this.fileToUpload, this.numberOfQueuesToImport).subscribe(result => this.importResult = result)
  }

  downloadExampleFile() {
    this.importerService.downloadExampleFile().subscribe(blob => {
      saveAs(blob, "example import file.csv")
    })
  }
}

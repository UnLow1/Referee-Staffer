import {Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {saveAs} from 'file-saver';
import {ImporterService} from '../../service/importer.service';
import {ImportResponse} from '../../request/importResponse';
import {IconComponent} from '../common/icon/icon.component';

/**
 * Import data — drop-zone for the bulk CSV upload that backs the entire app.
 *
 * The backend requires a `numberOfQueuesToImport` parameter alongside the file (used
 * by ImporterService.importData to decide which rows to insert as matches with grades
 * vs upcoming-only), so the drop-zone carries a small extra control for it. The example
 * cards link to the single example CSV served via `/api/importer/example`.
 */
@Component({
  selector: 'app-importer',
  templateUrl: './importer.component.html',
  styleUrl: './importer.component.scss',
  imports: [FormsModule, IconComponent]
})
export class ImporterComponent {
  private readonly importerService = inject(ImporterService);

  readonly fileToUpload = signal<File | null>(null);
  readonly numberOfQueuesToImport = signal<number | null>(null);
  readonly importResult = signal<ImportResponse | null>(null);
  readonly uploading = signal(false);
  readonly importError = signal<string | null>(null);

  handleFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fileToUpload.set(input.files?.item(0) ?? null);
    this.importResult.set(null);
    this.importError.set(null);
  }

  setNumberOfQueues(value: number | null): void {
    this.numberOfQueuesToImport.set(value);
  }

  upload(): void {
    const file = this.fileToUpload();
    const queues = this.numberOfQueuesToImport();
    // Template's [disabled] guards this, but TS can't see that — guard explicitly.
    if (file === null || queues === null) return;

    this.uploading.set(true);
    this.importError.set(null);
    this.importerService.postFile(file, queues).subscribe({
      next: result => {
        this.importResult.set(result);
        this.uploading.set(false);
      },
      error: () => {
        this.importError.set('Import failed — check that the CSV format matches the example.');
        this.uploading.set(false);
      }
    });
  }

  downloadExample(): void {
    this.importerService.downloadExampleFile().subscribe(blob => {
      saveAs(blob, 'example import file.csv');
    });
  }
}

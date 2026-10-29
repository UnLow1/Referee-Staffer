import type {MockedObject} from 'vitest';
import {HttpErrorResponse} from '@angular/common/http';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {of, Subject, throwError} from 'rxjs';
import {ImporterComponent} from './importer.component';
import {ImporterService} from '../../service/importer.service';
import {ImportResponse} from '../../request/importResponse';
import {createMock} from '../../testing/mock';

describe('ImporterComponent', () => {
  let fixture: ComponentFixture<ImporterComponent>;
  let component: ImporterComponent;
  let importerService: MockedObject<ImporterService>;

  const csv = new File(['a;b;c'], 'season.csv', {type: 'text/csv'});
  const response = {} as ImportResponse;

  beforeEach(async () => {
    importerService = createMock<ImporterService>(['postFile', 'downloadExampleFile']);
    await TestBed.configureTestingModule({
      imports: [ImporterComponent],
      providers: [{provide: ImporterService, useValue: importerService}]
    }).compileComponents();
    fixture = TestBed.createComponent(ImporterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function selectFile(file: File | null): void {
    const event = {target: {files: {item: () => file}}} as unknown as Event;
    component.handleFileInput(event);
  }

  it('stores the picked file and clears the previous run outcome', () => {
    component.importResult.set(response);
    component.importError.set('old error');

    selectFile(csv);

    expect(component.fileToUpload()).toBe(csv);
    expect(component.importResult()).toBeNull();
    expect(component.importError()).toBeNull();
  });

  it('does not upload until both the file and the queue count are set', () => {
    component.upload();
    expect(importerService.postFile).not.toHaveBeenCalled();

    selectFile(csv);
    component.upload();
    expect(importerService.postFile).not.toHaveBeenCalled();

    importerService.postFile.mockReturnValue(of(response));
    component.setNumberOfQueues(30);
    component.upload();
    expect(importerService.postFile).toHaveBeenCalledWith(csv, 30);
  });

  it('tracks the uploading flag across a successful import', () => {
    const upload = new Subject<ImportResponse>();
    importerService.postFile.mockReturnValue(upload);
    selectFile(csv);
    component.setNumberOfQueues(30);

    component.upload();
    expect(component.uploading()).toBe(true);

    upload.next(response);
    expect(component.uploading()).toBe(false);
    expect(component.importResult()).toBe(response);
    expect(component.importError()).toBeNull();
  });

  it('surfaces a friendly message when the import fails', () => {
    importerService.postFile.mockReturnValue(throwError(() => new Error('500')));
    selectFile(csv);
    component.setNumberOfQueues(30);

    component.upload();

    expect(component.uploading()).toBe(false);
    expect(component.importResult()).toBeNull();
    expect(component.importError()).toContain('Import failed');
  });

  it('shows the row-level detail the backend reports for a malformed CSV', () => {
    // RS-77: the importer validates the whole CSV and rejects it with an RFC 7807 ProblemDetail
    // naming the offending row. That detail is the deliverable, so it must reach the user as-is.
    const detail = 'Exception occurred while importing file with name season.csv ' +
      '(row 3: date must match dd.MM.yyyy HH:mm but was "NOTADATE")';
    importerService.postFile.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: {type: 'about:blank', title: 'Bad Request', status: 400, detail}
    })));
    selectFile(csv);
    component.setNumberOfQueues(30);

    component.upload();

    expect(component.importError()).toBe(detail);
    expect(component.uploading()).toBe(false);
  });

  it('falls back to the generic message when the failure carries no problem detail', () => {
    importerService.postFile.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: {type: 'about:blank', title: 'Bad Request', status: 400, detail: '  '}
    })));
    selectFile(csv);
    component.setNumberOfQueues(30);

    component.upload();

    expect(component.importError()).toContain('Import failed');
  });

  it('clears a previous error when retrying', () => {
    importerService.postFile.mockReturnValue(throwError(() => new Error('500')));
    selectFile(csv);
    component.setNumberOfQueues(30);
    component.upload();
    expect(component.importError()).not.toBeNull();

    importerService.postFile.mockReturnValue(of(response));
    component.upload();

    expect(component.importError()).toBeNull();
    expect(component.importResult()).toBe(response);
  });
});

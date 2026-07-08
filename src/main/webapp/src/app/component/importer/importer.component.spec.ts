import {ComponentFixture, TestBed} from '@angular/core/testing';
import {of, Subject, throwError} from 'rxjs';
import {ImporterComponent} from './importer.component';
import {ImporterService} from '../../service/importer.service';
import {ImportResponse} from '../../request/importResponse';

describe('ImporterComponent', () => {
  let fixture: ComponentFixture<ImporterComponent>;
  let component: ImporterComponent;
  let importerService: jasmine.SpyObj<ImporterService>;

  const csv = new File(['a;b;c'], 'season.csv', {type: 'text/csv'});
  const response = {} as ImportResponse;

  beforeEach(async () => {
    importerService = jasmine.createSpyObj('ImporterService', ['postFile', 'downloadExampleFile']);
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

    importerService.postFile.and.returnValue(of(response));
    component.setNumberOfQueues(30);
    component.upload();
    expect(importerService.postFile).toHaveBeenCalledWith(csv, 30);
  });

  it('tracks the uploading flag across a successful import', () => {
    const upload = new Subject<ImportResponse>();
    importerService.postFile.and.returnValue(upload);
    selectFile(csv);
    component.setNumberOfQueues(30);

    component.upload();
    expect(component.uploading()).toBeTrue();

    upload.next(response);
    expect(component.uploading()).toBeFalse();
    expect(component.importResult()).toBe(response);
    expect(component.importError()).toBeNull();
  });

  it('surfaces a friendly message when the import fails', () => {
    importerService.postFile.and.returnValue(throwError(() => new Error('500')));
    selectFile(csv);
    component.setNumberOfQueues(30);

    component.upload();

    expect(component.uploading()).toBeFalse();
    expect(component.importResult()).toBeNull();
    expect(component.importError()).toContain('Import failed');
  });

  it('clears a previous error when retrying', () => {
    importerService.postFile.and.returnValue(throwError(() => new Error('500')));
    selectFile(csv);
    component.setNumberOfQueues(30);
    component.upload();
    expect(component.importError()).not.toBeNull();

    importerService.postFile.and.returnValue(of(response));
    component.upload();

    expect(component.importError()).toBeNull();
    expect(component.importResult()).toBe(response);
  });
});

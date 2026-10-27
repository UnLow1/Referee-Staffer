import {InjectionToken} from '@angular/core';
import {saveAs} from 'file-saver';

/**
 * The browser download behind the importer's example CSV and the Staffer's assignment
 * PDF — `saveAs` from `file-saver`, injected rather than imported so a spec can swap it
 * through TestBed. jsdom implements none of the download machinery, and module mocking
 * is not an option under the Angular unit-test builder: it bundles the spec first, so
 * Vitest 5 no longer sees `vi.mock` at the module's top level and rejects it.
 */
export const SAVE_FILE = new InjectionToken<typeof saveAs>('SAVE_FILE', {
  providedIn: 'root',
  factory: () => saveAs
});

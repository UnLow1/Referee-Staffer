import {TestBed} from '@angular/core/testing';
import {saveAs} from 'file-saver';
import {SAVE_FILE} from './save-file.token';

describe('SAVE_FILE', () => {
  it('defaults to file-saver\'s saveAs, so components keep the real download unless a spec swaps it', () => {
    expect(TestBed.inject(SAVE_FILE)).toBe(saveAs);
  });
});

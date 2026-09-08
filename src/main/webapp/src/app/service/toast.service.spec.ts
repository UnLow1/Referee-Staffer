import {TestBed, fakeAsync, tick} from '@angular/core/testing';
import {ToastService} from './toast.service';

describe('ToastService', () => {
  const HIDE_AFTER_MS = 4500;

  let service: ToastService;

  beforeEach(() => {
    service = TestBed.inject(ToastService);
  });

  it('starts with no toast', () => {
    expect(service.toast()).toBeNull();
  });

  it('shows a toast with the default info kind', fakeAsync(() => {
    service.show('Import finished');

    expect(service.toast()).toEqual({message: 'Import finished', kind: 'info'});
    tick(HIDE_AFTER_MS);
  }));

  it('shows an error toast when asked', fakeAsync(() => {
    service.show('Request failed (HTTP 500)', 'error');

    expect(service.toast()).toEqual({message: 'Request failed (HTTP 500)', kind: 'error'});
    tick(HIDE_AFTER_MS);
  }));

  it('auto-hides after the timeout, not before', fakeAsync(() => {
    service.show('Import finished');

    tick(HIDE_AFTER_MS - 1);
    expect(service.toast()).not.toBeNull();

    tick(1);
    expect(service.toast()).toBeNull();
  }));

  it('replaces the current toast and restarts the hide timer', fakeAsync(() => {
    service.show('First');
    tick(HIDE_AFTER_MS - 1000);

    service.show('Second', 'error');
    expect(service.toast()).toEqual({message: 'Second', kind: 'error'});

    // The first toast's timer would have fired here; the restarted one must not.
    tick(HIDE_AFTER_MS - 1);
    expect(service.toast()).toEqual({message: 'Second', kind: 'error'});

    tick(1);
    expect(service.toast()).toBeNull();
  }));
});

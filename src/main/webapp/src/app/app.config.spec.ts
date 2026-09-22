import {NgZone, provideZonelessChangeDetection} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {appConfig} from './app.config';

describe('appConfig', () => {
  // RS-116: `bootstrapApplication` prepends zoneless change detection to the application
  // providers, so the app only gets a zone if it asks for one — and every component here
  // is ChangeDetectionStrategy.Eager, relying on the zone to repaint after an HTTP
  // callback. Reproducing that provider order is what makes this assertion meaningful:
  // TestBed is zone-based by default, so appConfig alone would pass either way.
  it('opts the application into zone-based change detection', () => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), ...appConfig.providers]
    });

    // A real NgZone runs callbacks inside the Angular zone; the zoneless NoopNgZone
    // just invokes them.
    expect(TestBed.inject(NgZone).run(() => NgZone.isInAngularZone())).toBe(true);
  });
});

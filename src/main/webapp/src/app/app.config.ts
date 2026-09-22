import {ApplicationConfig, importProvidersFrom, provideZoneChangeDetection} from '@angular/core';
import {provideHttpClient, withInterceptors, withXhr} from '@angular/common/http';
import {provideRouter} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {routes} from './app.routes';
import {httpErrorInterceptor} from './service/http-error.interceptor';

/**
 * Root application providers. Kept out of `main.ts` so they can be asserted on — see
 * `app.config.spec.ts`, which reproduces the provider order `bootstrapApplication` uses.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // Zone-based change detection, explicitly. Angular 22's `bootstrapApplication`
    // prepends `provideZonelessChangeDetectionInternal()` to the application providers,
    // so an app that never asks for a zone runs ZONELESS — even with zone.js loaded from
    // polyfills.ts. This app is zone-era throughout (every component is
    // ChangeDetectionStrategy.Eager, i.e. CheckAlways), so without this provider nothing
    // schedules a tick for a plain field mutated in an HTTP callback, and the view
    // silently never repaints (RS-116: the match drawer's selects stayed empty). Drop it
    // only as part of a deliberate zoneless migration that puts every asynchronously
    // filled component field on a signal.
    provideZoneChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withXhr(), withInterceptors([httpErrorInterceptor])),
    // FormsModule still needed — forms are template-driven by deliberate convention
    // (see CLAUDE.md, "Frontend forms & routing convention").
    importProvidersFrom(FormsModule),
  ]
};

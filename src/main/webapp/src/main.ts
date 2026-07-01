import {enableProdMode, importProvidersFrom} from '@angular/core';

import {environment} from './environments/environment';
import {provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {bootstrapApplication} from '@angular/platform-browser';
import {provideRouter} from '@angular/router';
import {routes} from './app/app.routes';
import {FormsModule} from '@angular/forms';
import {AppComponent} from './app/app.component';

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    // FormsModule still needed — forms are template-driven by deliberate convention
    // (see CLAUDE.md, "Frontend forms & routing convention").
    importProvidersFrom(FormsModule),
  ]
}).catch(err => console.error(err));

import { enableProdMode, importProvidersFrom } from '@angular/core';

import { environment } from './environments/environment';
import { RefereeService } from './app/service/referee.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { BrowserModule, bootstrapApplication } from '@angular/platform-browser';
import { RouterModule } from '@angular/router';
import { AppRoutingModule } from './app/app.routes';
import { MatDialogModule } from '@angular/material/dialog';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { AppComponent } from './app/app.component';

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, {
    providers: [
        importProvidersFrom(BrowserModule, RouterModule, AppRoutingModule, MatDialogModule, BrowserAnimationsModule, FormsModule),
        RefereeService, provideHttpClient(withInterceptorsFromDi())
    ]
})
  .catch(err => console.error(err));


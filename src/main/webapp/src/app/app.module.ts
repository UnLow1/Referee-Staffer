import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {RouterModule} from "@angular/router";
import {RefereeListComponent} from './component/referee-list/referee-list.component';
import {RefereeFormComponent} from './component/referee-form/referee-form.component';
import {RefereeService} from "./service/referee.service";
import {AppRoutingModule} from "./app.routes";
import {FormsModule} from "@angular/forms";
import {HttpClientModule} from "@angular/common/http";
import { MatchListComponent } from './component/match-list/match-list.component';
import { MatchFormComponent } from './component/match-form/match-form.component';
import { GradeListComponent } from './component/grade-list/grade-list.component';

@NgModule({
  declarations: [
    AppComponent,
    RefereeListComponent,
    RefereeFormComponent,
    MatchListComponent,
    MatchFormComponent,
    GradeListComponent
  ],
  imports: [
    BrowserModule,
    RouterModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule
  ],
  providers: [RefereeService],
  bootstrap: [AppComponent]
})
export class AppModule {
}

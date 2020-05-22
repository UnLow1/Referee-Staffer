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
import { TeamListComponent } from './component/team-list/team-list.component';
import { TeamFormComponent } from './component/team-form/team-form.component';
import { StafferComponent } from './component/staffer/staffer.component';
import { ImporterComponent } from './component/importer/importer.component';
import { StandingsComponent } from './component/standings/standings.component';
import { ExcludeValuePipe } from './pipe/exclude-value.pipe';

@NgModule({
  declarations: [
    AppComponent,
    RefereeListComponent,
    RefereeFormComponent,
    MatchListComponent,
    MatchFormComponent,
    GradeListComponent,
    TeamListComponent,
    TeamFormComponent,
    StafferComponent,
    ImporterComponent,
    StandingsComponent,
    ExcludeValuePipe
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

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
import {MatchListComponent} from './component/match-list/match-list.component';
import {MatchFormComponent} from './component/match-form/match-form.component';
import {GradeListComponent} from './component/grade-list/grade-list.component';
import {TeamListComponent} from './component/team-list/team-list.component';
import {TeamFormComponent} from './component/team-form/team-form.component';
import {StafferComponent} from './component/staffer/staffer.component';
import {ImporterComponent} from './component/importer/importer.component';
import {StandingsComponent} from './component/standings/standings.component';
import {ExcludeValuePipe} from './pipe/exclude-value.pipe';
import {ConfigurationComponent} from './component/configuration/configuration.component';
import {MatDialogModule} from "@angular/material/dialog";
import {InfoModalComponent} from './component/modals/info-modal/info-modal.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

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
    ExcludeValuePipe,
    ConfigurationComponent,
    InfoModalComponent
  ],
  imports: [
    BrowserModule,
    RouterModule,
    AppRoutingModule,
    HttpClientModule,
    MatDialogModule,
    BrowserAnimationsModule,
    FormsModule
  ],
  providers: [RefereeService],
  bootstrap: [AppComponent]
})
export class AppModule {
}

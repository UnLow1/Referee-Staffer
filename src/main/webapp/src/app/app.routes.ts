import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { RefereeListComponent } from './component/referee-list/referee-list.component';
import { RefereeFormComponent } from './component/referee-form/referee-form.component';

const routes: Routes = [
  { path: 'referees', component: RefereeListComponent },
  { path: 'addReferee', component: RefereeFormComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

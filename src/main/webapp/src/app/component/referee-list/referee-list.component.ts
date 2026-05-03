import { Component, OnInit, inject } from '@angular/core';
import {RefereeService} from "../../service/referee.service";
import {Referee} from "../../model/referee";
import { Router, RouterLink } from "@angular/router";
import { EditButtonComponent } from '../common/button/edit-button/edit-button.component';
import { DeleteButtonComponent } from '../common/button/delete-button/delete-button.component';
import { AddButtonComponent } from '../common/button/add-button/add-button.component';

@Component({
    selector: 'app-referee-list',
    templateUrl: './referee-list.component.html',
    styleUrls: ['./referee-list.component.scss'],
    imports: [EditButtonComponent, DeleteButtonComponent, AddButtonComponent, RouterLink]
})
export class RefereeListComponent implements OnInit {
  private router = inject(Router);
  private refereeService = inject(RefereeService);


  referees: Referee[]

  ngOnInit() {
    this.refereeService.findAll().subscribe(referees => this.referees = referees)
  }

  editReferee(referee: Referee) {
    this.router.navigate(['addReferee', referee.id])
  }

  deleteReferee(refereeToDelete: Referee) {
    this.refereeService.delete(refereeToDelete.id).subscribe(() =>
      this.referees = this.referees.filter((referee: Referee) => referee.id !== refereeToDelete.id))
  }
}

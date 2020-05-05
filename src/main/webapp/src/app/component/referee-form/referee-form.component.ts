import {Component, OnInit} from '@angular/core';
import {Referee} from "../../model/referee";
import {ActivatedRoute, Router} from "@angular/router";
import {RefereeService} from "../../service/referee.service";

@Component({
  selector: 'app-referee-form',
  templateUrl: './referee-form.component.html',
  styleUrls: ['./referee-form.component.css']
})
export class RefereeFormComponent implements OnInit {

  referee: Referee

  constructor(private route: ActivatedRoute, private router: Router, private refereeService: RefereeService) {
    this.referee = new Referee()
  }

  ngOnInit() {
  }

  onSubmit() {
    this.refereeService.save(this.referee).subscribe(result => this.gotoRefereesList())
  }

  gotoRefereesList() {
    this.router.navigate(['/referees'])
  }
}

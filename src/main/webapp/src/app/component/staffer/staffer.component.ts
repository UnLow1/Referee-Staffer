import { Component, OnInit } from '@angular/core';
import {StafferService} from "../../service/staffer.service";

@Component({
  selector: 'app-staffer',
  templateUrl: './staffer.component.html',
  styleUrls: ['./staffer.component.css']
})
export class StafferComponent implements OnInit {

  queue: number

  constructor(private stafferService: StafferService) { }

  ngOnInit(): void {
  }

  onSubmit() {
    this.stafferService.staffReferees(this.queue).subscribe(matches => console.log(matches))
  }
}

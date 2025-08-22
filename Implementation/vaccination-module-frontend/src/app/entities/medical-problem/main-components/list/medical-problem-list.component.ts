/**
 * Copyright (c) 2023 eHealth Suisse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the “Software”), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import { Component, inject, Input, OnInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { combineLatest } from 'rxjs';
import { IMedicalProblem } from '../../../../model';
import { ListHeaderComponent, MapperService, TableWrapperComponent, trackLangChange } from '../../../../shared';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';
import { filterPredicateExcludeJSONField, initializeActionData } from '../../../../shared/function';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { MedicalProblemService } from '../../service/medical-problem.service';

@Component({
  selector: 'vm-problem-list',
  standalone: true,
  templateUrl: './medical-problem-list.component.html',
  styleUrls: ['./medical-problem-list.component.scss'],
  imports: [SharedLibsModule, ListHeaderComponent, TableWrapperComponent],
})
export class MedicalProblemListComponent extends BreakPointSensorComponent implements OnInit {
  router: Router = inject(Router);
  problems!: MatTableDataSource<IMedicalProblem>;
  problemService: MedicalProblemService = inject(MedicalProblemService);
  problemData$ = combineLatest([this.problemService.query(), trackLangChange()]);
  mapper: MapperService = inject(MapperService);
  sharedDataService: SharedDataService = inject(SharedDataService);

  @Input() toggleHeader: boolean = true;
  @Input() displayedColumns: string[] = ['entryStatus', 'recordedDate', 'medicalProblemCode', 'clinicalStatus', 'recorder'];
  @Input() buttonVisibility: boolean = true;
  @Input() subtitleVisibility: boolean = true;
  @Input() showPatientName: boolean = true;
  @Input() isUpdated: boolean = false;
  @Input() tableWidth: string = '80vw';

  ngOnInit(): void {
    this.displayMenu(false, false);
    initializeActionData('', this.sharedDataService);
    this.problemData$.subscribe(([v]) => {
      this.problems = new MatTableDataSource<IMedicalProblem>(this.mapper.problemTranslateMapper(v));
      this.problems.filterPredicate = filterPredicateExcludeJSONField;
      return this.problems;
    });
  }

  navigateToDetails(row: IMedicalProblem): void {
    this.router.navigateByUrl(`medical-problem/${row.id}/detail`);
  }

  addNewRecord(): void {
    this.router.navigateByUrl('medical-problem/new');
  }

  filter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value.toLowerCase();
    this.problems.filter = filterValue.trim().toLowerCase();
  }
}

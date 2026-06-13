/**
 * Copyright (c) 2026 eHealth Suisse
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
import { Component, inject, input, OnInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { combineLatest } from 'rxjs';
import { filterPredicateExcludeJSONField, initializeActionData } from 'src/app/shared/function/functions';
import { ILaboratorySerology } from '../../../../model';
import { ListHeaderComponent, MapperService, TableWrapperComponent, trackLangChange } from '../../../../shared';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';
import { LaboratorySerologyService } from '../../services/laboratory-serology.service';

@Component({
  selector: 'vm-laboratory-serology-list',
  standalone: true,
  templateUrl: './laboratory-serology-list.component.html',
  styleUrls: ['./laboratory-serology-list.component.scss'],
  imports: [SharedLibsModule, ListHeaderComponent, TableWrapperComponent],
})
export class LaboratorySerologyListComponent extends BreakPointSensorComponent implements OnInit {
  router: Router = inject(Router);
  laboratorySerologies!: MatTableDataSource<ILaboratorySerology>;
  laboratorySerologyService: LaboratorySerologyService = inject(LaboratorySerologyService);
  laboratorySerologyData$ = combineLatest([this.laboratorySerologyService.query(), trackLangChange()]);
  mapper: MapperService = inject(MapperService);
  sharedDataService: SharedDataService = inject(SharedDataService);

  toggleHeader = input(true);
  displayedColumns = input<string[]>(['entryStatus', 'recordedDate', 'laboratorySerologyCode', 'value', 'recorder', 'comment']);
  buttonVisibility = input(true);
  subtitleVisibility = input(true);
  isEmbedded = input(false);
  showPatientName = input(true);
  isUpdated = input(true);
  tableWidth = input('80vw');

  filter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value.toLowerCase();
    this.laboratorySerologies.filter = filterValue.trim().toLowerCase();
  }

  ngOnInit(): void {
    this.displayMenu(false, false);
    initializeActionData('', this.sharedDataService);
    this.laboratorySerologyData$.subscribe(([v]) => {
      this.laboratorySerologies = new MatTableDataSource<ILaboratorySerology>(this.mapper.laboratorySerologyTranslateMapper(v));
      this.laboratorySerologies.filterPredicate = filterPredicateExcludeJSONField;
      return this.laboratorySerologies;
    });
  }

  navigateToDetails(row: ILaboratorySerology): void {
    this.router.navigate(['laboratory-serology', row.id, 'detail']);
  }

  addNewRecord(): void {
    this.router.navigateByUrl('laboratory-serology/new');
  }
}

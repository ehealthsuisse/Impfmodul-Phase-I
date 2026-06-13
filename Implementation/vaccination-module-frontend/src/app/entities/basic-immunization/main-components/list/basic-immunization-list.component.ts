/**
 * Copyright (c) 2026 eHealth Suisse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import { Component, inject, input, OnInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { combineLatest } from 'rxjs';
import { IBasicImmunization } from '../../../../model';
import { ListHeaderComponent, MapperService, TableWrapperComponent, trackLangChange } from '../../../../shared';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';
import { filterPredicateExcludeJSONField, initializeActionData } from '../../../../shared/function';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { BasicImmunizationService } from '../../service/basic-immunization.service';

@Component({
  selector: 'vm-basic-immunization-list',
  templateUrl: './basic-immunization-list.component.html',
  styleUrls: ['./basic-immunization-list.component.scss'],
  imports: [SharedLibsModule, ListHeaderComponent, TableWrapperComponent],
})
export class BasicImmunizationListComponent extends BreakPointSensorComponent implements OnInit {
  router: Router = inject(Router);
  basicImmunizations!: MatTableDataSource<IBasicImmunization>;
  basicImmunizationService: BasicImmunizationService = inject(BasicImmunizationService);
  basicImmunizationData$ = combineLatest([this.basicImmunizationService.query(), trackLangChange()]);
  mapper: MapperService = inject(MapperService);
  sharedDataService: SharedDataService = inject(SharedDataService);

  toggleHeader = input<boolean>(true);
  displayedColumns = input<string[]>(['entryStatus', 'onsetDate', 'basicImmunizationCode', 'comment']);
  buttonVisibility = input<boolean>(true);
  subtitleVisibility = input<boolean>(true);
  showPatientName = input<boolean>(true);
  isUpdated = input<boolean>(false);
  tableWidth = input<string>('80vw');

  ngOnInit(): void {
    this.displayMenu(false, false);
    initializeActionData('', this.sharedDataService);
    this.basicImmunizationData$.subscribe(([v]) => {
      this.basicImmunizations = new MatTableDataSource<IBasicImmunization>(this.mapper.basicImmunizationTranslateMapper(v));
      this.basicImmunizations.filterPredicate = filterPredicateExcludeJSONField;
      return this.basicImmunizations;
    });
  }

  navigateToDetails(row: IBasicImmunization): void {
    this.router.navigateByUrl(`basic-immunization/${row.id}/detail`);
  }

  addNewRecord(): void {
    this.router.navigateByUrl('basic-immunization/new');
  }

  filter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value.toLowerCase();
    this.basicImmunizations.filter = filterValue.trim().toLowerCase();
  }
}

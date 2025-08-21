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
import { filterPredicateExcludeJSONField, initializeActionData } from 'src/app/shared/function/functions';
import { ListHeaderComponent, MapperService, TableWrapperComponent, trackLangChange } from '../../../../shared';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { VaccinationService } from '../../services/vaccination.service';
import { IVaccination } from '../../../../model';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';

@Component({
  selector: 'vm-vaccination-list',
  standalone: true,
  templateUrl: './vaccination-list.component.html',
  styleUrls: ['./vaccination-list.component.scss'],
  imports: [SharedLibsModule, TableWrapperComponent, ListHeaderComponent],
})
export class VaccinationListComponent extends BreakPointSensorComponent implements OnInit {
  vaccinations!: MatTableDataSource<IVaccination>;
  sharedDataService: SharedDataService = inject(SharedDataService);

  @Input() toggleHeader: boolean = true;
  @Input() vaccinationColumns: string[] = ['entryStatus', 'occurrenceDate', 'vaccineCode', 'targetDiseases', 'doseNumber', 'recorder'];
  @Input() buttonVisibility: boolean = true;
  @Input() subtitleVisibility: boolean = true;
  @Input() isEmbedded: boolean = false;
  @Input() showPatientName: boolean = true;
  @Input() isUpdated: boolean = true;
  @Input() tableWidth: string = '80vw';

  router: Router = inject(Router);
  vaccinationService: VaccinationService = inject(VaccinationService);
  mapper: MapperService = inject(MapperService);
  vaccinationData$ = combineLatest([this.vaccinationService.query(), trackLangChange()]);

  ngOnInit(): void {
    this.displayMenu(false, false);

    initializeActionData('', this.sharedDataService);
    this.vaccinationData$.subscribe(([v]) => {
      this.vaccinations = new MatTableDataSource<IVaccination>(this.mapper.vaccinationTranslateMapper(v));
      this.vaccinations.filterPredicate = filterPredicateExcludeJSONField;
      return this.vaccinations;
    });
  }

  addNewRecord(): void {
    this.router.navigateByUrl('vaccination/new');
  }

  filterVaccinations(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value.toLowerCase();
    this.vaccinations.filter = filterValue.trim().toLowerCase();
  }

  navigateToDetails(row: IVaccination): void {
    this.router.navigate(['vaccination', row.id, 'detail']);
  }
}

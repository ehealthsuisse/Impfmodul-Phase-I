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
import { IInfectiousDiseases } from '../../../../model';
import { ListHeaderComponent, MapperService, TableWrapperComponent, trackLangChange } from '../../../../shared';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { InfectiousDiseasesService } from '../../service/infectious-diseases.service';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';

@Component({
  selector: 'vm-infectious-diseases-list',
  standalone: true,
  templateUrl: './infectious-diseases-list.component.html',
  styleUrls: ['./infectious-diseases-list.component.scss'],
  imports: [SharedLibsModule, ListHeaderComponent, TableWrapperComponent],
})
export class InfectiousDiseasesListComponent extends BreakPointSensorComponent implements OnInit {
  router: Router = inject(Router);
  infectiousDiseases!: MatTableDataSource<IInfectiousDiseases>;
  infectiousDiseasesService: InfectiousDiseasesService = inject(InfectiousDiseasesService);
  infectiousDiseasesData$ = combineLatest([this.infectiousDiseasesService.query(), trackLangChange()]);
  mapper: MapperService = inject(MapperService);
  sharedDataService: SharedDataService = inject(SharedDataService);

  @Input() toggleHeader: boolean = true;
  @Input() displayedColumns: string[] = ['entryStatus', 'recordedDate', 'illnessCode', 'recorder'];
  @Input() buttonVisibility: boolean = true;
  @Input() subtitleVisibility: boolean = true;
  @Input() isEmbedded: boolean = false;
  @Input() showPatientName: boolean = true;
  @Input() isUpdated: boolean = false;
  @Input() tableWidth: string = '80vw';

  ngOnInit(): void {
    this.displayMenu(false, false);

    initializeActionData('', this.sharedDataService);
    this.infectiousDiseasesData$.subscribe(([v]) => {
      this.infectiousDiseases = new MatTableDataSource<IInfectiousDiseases>(this.mapper.illnessesTranslateMapper(v));
      this.infectiousDiseases.filterPredicate = filterPredicateExcludeJSONField;
      return this.infectiousDiseases;
    });
  }

  navigateToDetails(row: IInfectiousDiseases): void {
    this.router.navigateByUrl(`infectious-diseases/${row.id}/detail`);
  }

  addNewRecord(): void {
    this.router.navigateByUrl('infectious-diseases/new');
  }

  filter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value.toLowerCase();
    this.infectiousDiseases.filter = filterValue.trim().toLowerCase();
  }
}

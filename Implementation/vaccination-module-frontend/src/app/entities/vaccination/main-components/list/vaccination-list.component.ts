/**
 * Copyright (c) 2022 eHealth Suisse
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
import { MatSortModule } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { combineLatest } from 'rxjs';
import { ApplicationConfigService } from '../../../../core';
import { Vaccination } from '../../../../model';
import {
  DialogService,
  GenericButtonComponent,
  HelpButtonComponent,
  ListHeaderComponent,
  MainWrapperComponent,
  MapperService,
  PageTitleTranslateComponent,
  TableWrapperComponent,
  trackLangChange,
} from '../../../../shared';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { VaccinationService } from '../../services/vaccination.service';

@Component({
  selector: 'vm-vaccination-list',
  templateUrl: './vaccination-list.component.html',
  styleUrls: ['./vaccination-list.component.scss'],
  standalone: true,
  imports: [
    SharedLibsModule,
    TableWrapperComponent,
    GenericButtonComponent,
    TranslateModule,
    HelpButtonComponent,
    ListHeaderComponent,
    MainWrapperComponent,
    MatSortModule,
  ],
})
export class VaccinationListComponent extends PageTitleTranslateComponent implements OnInit {
  router: Router = inject(Router);
  dialog = inject(DialogService);
  appConfig = inject(ApplicationConfigService);
  vaccinations!: MatTableDataSource<Vaccination>;
  vaccinationService: VaccinationService = inject(VaccinationService);
  mapper: MapperService = inject(MapperService);
  vaccinationData$ = combineLatest([this.vaccinationService.query(), trackLangChange()]);
  canValidated!: boolean;

  @Input() toggleHeader: boolean = true;
  @Input() vaccinationColumns: string[] = ['entryStatus', 'occurrenceDate', 'vaccineCode', 'targetDiseases', 'doseNumber', 'recorder'];
  @Input() buttonVisibility: boolean = true;
  @Input() subtitleVisibility: boolean = true;
  @Input() isEmbedded: boolean = false;
  @Input() showPatientName: boolean = true;
  @Input() isUpdated: boolean = true;
  @Input() tableWidth: string = '80vw';

  ngOnInit(): void {
    this.vaccinationData$.subscribe(
      ([v]) => (this.vaccinations = new MatTableDataSource<Vaccination>(this.mapper.vaccinationTranslateMapper(v)))
    );
  }

  addNewRecord(): void {
    this.router.navigateByUrl('vaccination/new');
  }

  filterVaccinations(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value.toLowerCase();
    this.vaccinations.filter = filterValue.trim().toLowerCase();
  }

  navigateToDetails(row: Vaccination): void {
    this.router.navigate(['vaccination', row.id, 'detail']);
  }

  filter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value.toLowerCase();
    this.vaccinations.filter = filterValue.trim().toLowerCase();
  }
}

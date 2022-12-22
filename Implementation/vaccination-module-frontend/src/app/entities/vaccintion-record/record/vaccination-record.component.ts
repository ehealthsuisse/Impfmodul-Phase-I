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
import { HttpClient } from '@angular/common/http';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FlexLayoutModule } from '@angular/flex-layout';
import { MatIconModule } from '@angular/material/icon';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { Allergy, IIllnesses, IVaccinationRecord, Vaccination } from '../../../model';
import {
  DialogService,
  GenericButtonComponent,
  HelpButtonComponent,
  IHumanDTO,
  MapperService,
  PageTitleTranslateComponent,
  TableWrapperComponent,
} from '../../../shared';
import { SharedLibsModule } from '../../../shared/shared-libs.module';
import { VaccinationListComponent } from '../../vaccination/main-components';
import { VaccinationRecordService } from '../service/vaccination-record.service';
import { downloadRecordValue } from '../../../shared/function';
import { SharedDataService } from '../../../shared/services/shared-data.service';

@Component({
  selector: 'vm-vaccination-record',
  standalone: true,
  imports: [
    SharedLibsModule,
    TableWrapperComponent,
    VaccinationListComponent,
    GenericButtonComponent,
    HelpButtonComponent,
    MatIconModule,
    FlexLayoutModule,
  ],
  templateUrl: './vaccination-record.component.html',
  styleUrls: ['./vaccination-record.component.scss'],
})
export class VaccinationRecordComponent extends PageTitleTranslateComponent implements OnInit, OnDestroy {
  router = inject(Router);
  http = inject(HttpClient);
  dialog = inject(DialogService);
  vaccinations!: MatTableDataSource<Vaccination>;
  illnesses!: MatTableDataSource<IIllnesses>;
  allergies!: MatTableDataSource<Allergy>;
  translationService: TranslateService = inject(TranslateService);
  mapper: MapperService = inject(MapperService);
  vaccinationRecordService = inject(VaccinationRecordService);
  allergyColumns: string[] = ['occurrenceDate', 'allergyCode', 'clinicalStatus'];
  illnessesColumns: string[] = ['recordedDate', 'illnessCode', 'clinicalStatus'];
  vaccinationColumns: string[] = ['occurrenceDate', 'vaccineCode', 'targetDiseases', 'doseNumber', 'recorder'];
  subscription!: Subscription;
  sharedDataService: SharedDataService = inject(SharedDataService);

  get patient(): IHumanDTO {
    return this.sharedDataService.storedData['patient']! ? this.sharedDataService.storedData['patient'] : null;
  }

  ngOnInit(): void {
    this.getRecord();
  }

  getRecord(): Subscription {
    this.spinnerService.show();
    return (this.subscription = this.vaccinationRecordService.queryOneRecord().subscribe({
      next: value => {
        this.sharedDataService.storedData['patient'] = value.patient;
        this.sharedDataService.setSessionStorage();
        this.vaccinations = new MatTableDataSource<Vaccination>(this.mapper.vaccinationTranslateMapper(value.vaccinations));
        this.illnesses = new MatTableDataSource<IIllnesses>(this.mapper.illnessesTranslateMapper(value.pastIllnesses));
        this.allergies = new MatTableDataSource<Allergy>(this.mapper.allergyTranslationMapper(value.allergies));
        this.spinnerService.hide();
      },
    }));
  }

  exportPdf(): any {
    this.spinnerService.show();
    return this.vaccinationRecordService.queryOneRecord().subscribe({
      next: record => {
        record.allergies.forEach(e => {
          e.allergyCode.name = this.translationService.instant('ALLERGY_NAMES.' + e.allergyCode.code);
          e.clinicalStatus.name = this.translationService.instant('ALLERGY_CLINICAL_STATUS.' + e.clinicalStatus.code);
        });
        record.pastIllnesses.forEach(e => {
          e.illnessCode.name = this.translationService.instant('ILLNESSES_CODE.' + e.illnessCode.code);
          e.clinicalStatus.name = this.translationService.instant('ILLNESS_CLINICAL_STATUS.' + e.clinicalStatus.code);
        });
        this.vaccinationRecordService.exportPdf(record).subscribe({
          next: response => {
            const blob = new Blob([response], { type: 'application/pdf' });
            const url = window.URL.createObjectURL(blob);
            /* eslint-disable-next-line security/detect-non-literal-fs-filename -- Safe as no value holds user input */
            window.open(url);
          },
        });
        this.spinnerService.hide();
      },
    });
  }

  save = (): Subscription => {
    this.spinnerService.show();
    return this.vaccinationRecordService.queryOneRecord().subscribe({
      next: record =>
        this.vaccinationRecordService.saveRecord(record!).subscribe({
          next: () => {
            this.dialog.openDialog('HELP.VACCINATION_RECORD.SAVE.TITLE', 'HELP.VACCINATION_RECORD.SAVE.BODY');
          },
          complete: () => {
            this.spinnerService.hide();
          },
        }),
    });
  };

  navigateToAllergy(row: Allergy): void {
    this.router.navigate(['allergy', row.id, 'detail']);
  }

  navigateToIllness(row: IIllnesses): void {
    this.router.navigate(['illnesses', row.id, 'detail']);
  }

  navigateToVaccination(row: Vaccination): void {
    this.router.navigate(['vaccination', row.id, 'detail']);
  }

  override ngOnDestroy(): void {
    this.subscription.unsubscribe();
    super.ngOnDestroy();
  }

  addVaccination(): void {
    this.router.navigate(['vaccination', 'new']);
  }

  addIllness(): void {
    this.router.navigate(['illnesses', 'new']);
  }

  addAllergy(): void {
    this.router.navigate(['allergy', 'new']);
  }

  download = (): Subscription => {
    return this.vaccinationRecordService.queryOneRecord().subscribe({
      next: record => downloadRecordValue<IVaccinationRecord>(record, this.patient),
    });
  };
}

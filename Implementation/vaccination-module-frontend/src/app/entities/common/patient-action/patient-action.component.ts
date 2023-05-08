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
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DATE_FORMAT, DialogService, IValueDTO } from '../../../shared';
import { TranslateService } from '@ngx-translate/core';
import { finalize, map, Observable, Subscription, switchMap, tap } from 'rxjs';
import { downloadRecordValue, openSnackBar } from '../../../shared/function';
import { IAdverseEvent, IInfectiousDiseases, IMedicalProblem, IVaccination, IVaccinationRecord } from '../../../model';
import { VaccinationRecordService } from '../../vaccintion-record/service/vaccination-record.service';
import { SpinnerService } from '../../../shared/services/spinner.service';
import { SharedLibsModule } from '../../../shared/shared-libs.module';
import { BreakPointSensorComponent } from '../../../shared/component/break-point-sensor/break-point-sensor.component';
import dayjs from 'dayjs';
import { SessionInfoService } from '../../../core/security/session-info.service';

@Component({
  selector: 'vm-patient-action',
  standalone: true,
  imports: [SharedLibsModule],
  templateUrl: './patient-action.component.html',
  styleUrls: ['./patient-action.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PatientActionComponent extends BreakPointSensorComponent {
  vaccinationRecordService: VaccinationRecordService = inject(VaccinationRecordService);
  spinnerService: SpinnerService = inject(SpinnerService);
  translationService: TranslateService = inject(TranslateService);
  dialog: DialogService = inject(DialogService);
  sessionInfoService: SessionInfoService = inject(SessionInfoService);

  download = (): Subscription => {
    return this.vaccinationRecordService.queryOneRecord().subscribe({
      next: record => downloadRecordValue<IVaccinationRecord>(record, this.sessionInfoService.author.getValue(), this.sessionInfoService),
    });
  };

  save(): Subscription {
    return this.queryRecord()
      .pipe(switchMap(record => this.saveRecord(record)))
      .subscribe();
  }

  exportPdf(): void {
    this.spinnerService.show();
    this.vaccinationRecordService.queryOneRecord().subscribe({
      next: record => {
        this.translateIllnesses(record.pastIllnesses);
        this.translateAllergies(record.allergies);
        this.translateMedicalProblem(record.medicalProblems);
        this.translateVaccination(record.vaccinations);
        this.exportPdfFromRecord(record);
        this.spinnerService.hide();
      },
    });
  }

  private queryRecord(): Observable<IVaccinationRecord> {
    return this.vaccinationRecordService.queryOneRecord();
  }

  private saveRecord(record: IVaccinationRecord): Observable<void> {
    this.spinnerService.show();
    return this.vaccinationRecordService.saveRecord(record).pipe(
      tap(() => openSnackBar(this.translationService, this.snackBar, 'HELP.VACCINATION_RECORD.SAVE.BODY')),

      finalize(() => this.spinnerService.hide()),
      map(() => {})
    );
  }

  // Translate the names of allergies
  private translateAllergies(allergies: IAdverseEvent[]): void {
    allergies.map(allergy => {
      allergy.code.name = this.translationService.instant(`ALLERGY_CODE.${allergy.code.code}`);
      allergy.clinicalStatus.name = this.translationService.instant(`ALLERGY_CLINICAL_STATUS.${allergy.clinicalStatus.code}`);
    });
  }

  // translate vaccination
  private translateVaccination(vaccinations: IVaccination[]): void {
    vaccinations.map(vaccination => {
      vaccination.code.name = this.translationService.instant(`VACCINATION_CODE.${vaccination.code.code}`);
      vaccination.status.name = this.translationService.instant(`VACCINATION_CLINICAL_STATUS.${vaccination.status.code}`);
    });
  }

  // Translate the names of past infectious_diseases
  private translateIllnesses(illnesses: IInfectiousDiseases[]): void {
    illnesses.map(illness => {
      illness.illnessCode.name = this.translationService.instant(`ILLNESSES_CODE.${illness.code.code}`);
      illness.clinicalStatus.name = this.translationService.instant(`ILLNESS_CLINICAL_STATUS.${illness.clinicalStatus.code}`);
    });
  }

  private translateMedicalProblem(medicalProblems: IMedicalProblem[]): void {
    medicalProblems.map(medicalProblem => {
      medicalProblem.code.name = this.translationService.instant(`MEDICAL_PROBLEM_CODE.${medicalProblem.code.code}`);
      medicalProblem.clinicalStatus.name = this.translationService.instant(
        `MEDICAL_PROBLEM_CLINICAL_STATUS.${medicalProblem.clinicalStatus.code}`
      );
    });
  }

  /**
   * Exports a PDF file for the given vaccination record.
   * @param record The vaccination record to export.
   */
  private exportPdfFromRecord(record: IVaccinationRecord): void {
    this.vaccinationRecordService
      .fetchTargetDisease()
      .pipe(
        map((res: IValueDTO[]) =>
          res.map(targetDisease => ({
            code: targetDisease.code,
            name: this.translationService.instant(`vaccination-targetdiseases.${targetDisease.code}`),
          }))
        ),
        tap((i18nTargetDiseases: IValueDTO[]) => {
          record.i18nTargetDiseases = i18nTargetDiseases;
        }),
        switchMap(() => this.vaccinationRecordService.exportPdf(record))
      )
      .subscribe(response => {
        const blob = new Blob([response], { type: 'application/pdf' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        const { prefix, lastName, firstName } = this.sessionInfoService.author.getValue();
        link.download = `${prefix ? prefix + '_' : ''}${lastName ? lastName + '_' : ''}${firstName ? firstName + '_' : ''}${dayjs().format(
          DATE_FORMAT
        )}.pdf`;

        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      });
  }
}

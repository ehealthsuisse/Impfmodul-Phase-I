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
import { ChangeDetectionStrategy, Component, ElementRef, HostListener, inject, OnInit } from '@angular/core';
import { DialogService, IValueDTO } from '../../../shared';
import { TranslateService } from '@ngx-translate/core';
import { finalize, map, Observable, Subscription, switchMap, tap } from 'rxjs';
import { downloadRecordValue, openSnackBar } from '../../../shared/function';
import { IAdverseEvent, IInfectiousDiseases, IMedicalProblem, IVaccination, IVaccinationRecord } from '../../../model';
import { filterPatientRecordData, VaccinationRecordService } from '../../vaccination-record/service/vaccination-record.service';
import { SpinnerService } from '../../../shared/services/spinner.service';
import { SharedLibsModule } from '../../../shared/shared-libs.module';
import { BreakPointSensorComponent } from '../../../shared/component/break-point-sensor/break-point-sensor.component';
import { SessionInfoService } from '../../../core/security/session-info.service';
import { PatientService } from '../../../shared/component/patient/patient.service';
import { ITranslationRequest } from '../../../model/translation-request.interface';

@Component({
  selector: 'vm-patient-action',
  standalone: true,
  imports: [SharedLibsModule],
  templateUrl: './patient-action.component.html',
  styleUrls: ['./patient-action.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PatientActionComponent extends BreakPointSensorComponent implements OnInit {
  vaccinationRecordService: VaccinationRecordService = inject(VaccinationRecordService);
  spinnerService: SpinnerService = inject(SpinnerService);
  translationService: TranslateService = inject(TranslateService);
  dialog: DialogService = inject(DialogService);
  sessionInfoService: SessionInfoService = inject(SessionInfoService);
  patientService: PatientService = inject(PatientService);
  elementRef: ElementRef = inject(ElementRef);
  isEmergencyMode: boolean = false;
  showMoreOptions: boolean = false;

  ngOnInit(): void {
    this.isEmergencyMode = this.sessionInfoService.isEmergencyMode();
  }

  download = (): Subscription => {
    this.showMoreOptions = false;
    return this.vaccinationRecordService.queryOneRecord().subscribe({
      next: record => {
        const url = 'data:text/plain;charset=utf-8,' + encodeURIComponent(record.json!);
        const jsonExtension = '.json';
        downloadRecordValue<IVaccinationRecord>(record, this.patientService, url, jsonExtension);
      },
    });
  };

  save(): Subscription {
    this.showMoreOptions = false;
    return this.queryRecord()
      .pipe(switchMap(record => this.saveRecord(record)))
      .subscribe();
  }

  exportPdf(): void {
    this.spinnerService.show();
    this.vaccinationRecordService.queryOneRecord().subscribe({
      next: record => {
        filterPatientRecordData(record);
        this.translateIllnesses(record.pastIllnesses);
        this.translateAllergies(record.allergies);
        this.translateMedicalProblem(record.medicalProblems);
        this.translateVaccination(record.vaccinations);
        this.exportPdfFromRecord(record);
        this.spinnerService.hide();
      },
    });
  }

  @HostListener('document:click', ['$event'])
  onOutsideClick(event: MouseEvent): void {
    const clickedInside = this.elementRef.nativeElement.contains(event.target);
    if (!clickedInside) {
      this.showMoreOptions = false;
    }
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
      vaccination.code.name = this.translationService.instant(`VACCINE_CODE.${vaccination.code.code}`);
      vaccination.vaccineCode.name = this.translationService.instant(`VACCINE_CODE.${vaccination.code.code}`);
      vaccination.status.name = this.translationService.instant(`VACCINATION_CLINICAL_STATUS.${vaccination.status.code}`);
    });
  }

  // Translate the names of past infectious_diseases
  private translateIllnesses(illnesses: IInfectiousDiseases[]): void {
    illnesses.map(illness => {
      illness.illnessCode.name = this.translationService.instant(`ILLNESS_CODE.${illness.code.code}`);
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
        map(res => this.mapTargetDiseases(res)),
        switchMap(targetDiseases => this.createTranslationRequest(targetDiseases, record))
      )
      .subscribe(response => this.handlePdfResponse(response, record));
  }

  private mapTargetDiseases(res: IValueDTO[]): IValueDTO[] {
    return res.map(targetDisease => ({
      code: targetDisease.code,
      name: this.translationService.instant(`vaccination-targetdiseases.${targetDisease.code}`),
      system: targetDisease.system,
    }));
  }

  private createTranslationRequest(targetDiseases: IValueDTO[], record: IVaccinationRecord): Observable<Blob> {
    const payload: ITranslationRequest = {
      targetDiseases,
      allergyCodes: record.allergies.map(item => item.code),
      illnessCodes: record.pastIllnesses.map(item => item.illnessCode),
      vaccineCodes: record.vaccinations.map(item => item.code),
      medicalProblemCodes: record.medicalProblems.map(item => item.code),
    };
    return this.vaccinationRecordService.exportPdf(payload, this.translateService.currentLang);
  }
  private handlePdfResponse(response: Blob, record: IVaccinationRecord): void {
    const blob = new Blob([response], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const pdfExtension = '.pdf';
    downloadRecordValue(record, this.patientService, url, pdfExtension);
  }
}

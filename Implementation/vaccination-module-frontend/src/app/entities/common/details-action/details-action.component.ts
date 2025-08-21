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
import { CommonModule, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
import { FlexModule } from '@angular/flex-layout';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { DialogService, IBaseDTO, IHumanDTO, TranslateDirective } from 'src/app/shared';
import { SessionInfoService } from '../../../core/security/session-info.service';
import { IAdverseEvent, IInfectiousDiseases, IMedicalProblem, IVaccination } from '../../../model';
import { BreakPointSensorComponent } from '../../../shared/component/break-point-sensor/break-point-sensor.component';
import { PatientService } from '../../../shared/component/patient/patient.service';
import { deleteRecord, downloadRecordValue } from '../../../shared/function';
import { SharedDataService } from '../../../shared/services/shared-data.service';
import { AdverseEventService } from '../../adverse_event/services/adverse-event.service';
import { InfectiousDiseasesService } from '../../infectious_diseases/service/infectious-diseases.service';
import { MedicalProblemService } from '../../medical-problem/service/medical-problem.service';
import { VaccinationService } from '../../vaccination/services/vaccination.service';

@Component({
  selector: 'vm-details-action',
  standalone: true,
  imports: [
    CommonModule,
    FlexModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTooltipModule,
    NgIf,
    TranslateModule,
    TranslateDirective,
  ],
  templateUrl: './details-action.component.html',
  styleUrls: ['./details-action.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetailsActionComponent extends BreakPointSensorComponent implements OnInit {
  details!: any;
  type: string = '';
  sharedDataService: SharedDataService = inject(SharedDataService);
  vaccinationService: VaccinationService = inject(VaccinationService);
  illnessService: InfectiousDiseasesService = inject(InfectiousDiseasesService);
  problemService: MedicalProblemService = inject(MedicalProblemService);
  adverseEventService: AdverseEventService = inject(AdverseEventService);
  matDialog: MatDialog = inject(MatDialog);
  router: Router = inject(Router);
  dialog: DialogService = inject(DialogService);
  sessionInfoService: SessionInfoService = inject(SessionInfoService);
  patientService: PatientService = inject(PatientService);
  isEmergencyMode: boolean = false;

  @Input() helpDialogTitle!: string;
  @Input() helpDialogBody!: string;

  canValidate: boolean = false;
  isEditOrDeleteDisabled: boolean = false;
  isValidateDisabled: boolean = false;
  itemId: string = '';

  get patient(): IHumanDTO | null {
    return this.sharedDataService.storedData['patient']! ? this.sharedDataService.storedData['patient'] : null;
  }

  ngOnInit(): void {
    this.updateType();
    const role = this.sessionInfoService.author.getValue().role;
    this.canValidate = role === 'HCP' || role === 'ASS';
    this.isEmergencyMode = this.sessionInfoService.isEmergencyMode();
    this.isEditOrDeleteDisabled = this.details?.updated || this.details?.deleted || this.isEmergencyMode;
    this.isValidateDisabled = this.details?.validated || this.isEditOrDeleteDisabled;
  }

  editRecord(): void {
    this.updateType();
    if (this.type === 'vaccination') {
      this.router.navigate([`vaccination`, this.itemId, 'edit']);
    }

    if (this.type === 'infectious-diseases') {
      this.router.navigate([`infectious-diseases`, this.itemId, 'edit']);
    }

    if (this.type === 'medical-problem') {
      this.router.navigate([`medical-problem`, this.itemId, 'edit']);
    }

    if (this.type === 'allergy') {
      this.router.navigate([`allergy`, this.itemId, 'edit']);
    }

    this.sharedDataService.showActionMenu = false;
  }

  deleteRecord(): void {
    this.updateType();
    const baseDetails = { ...this.details };
    this.sharedDataService.showActionMenu = false;
    const deleteMessage = this.translateService.instant('GLOBAL.DELETE_CONFIRMATION_MESSAGE');
    switch (this.type) {
      case 'vaccination':
        deleteRecord(this.matDialog, this.vaccinationService, baseDetails, deleteMessage);
        break;
      case 'infectious-diseases':
        deleteRecord(this.matDialog, this.illnessService, baseDetails, deleteMessage);
        break;
      case 'medical-problem':
        deleteRecord(this.matDialog, this.problemService, baseDetails, deleteMessage);
        break;
      case 'allergy':
        deleteRecord(this.matDialog, this.adverseEventService, baseDetails, deleteMessage);
        break;
    }
  }
  download(): void {
    this.updateType();
    this.sharedDataService.showActionMenu = false;
    const url = 'data:text/plain;charset=utf-8,' + encodeURIComponent(this.details.json!);
    const jsonExtension = '.json';
    switch (this.type) {
      case 'vaccination':
        downloadRecordValue<IVaccination>(this.details, this.patientService, url, jsonExtension);
        break;
      case 'infectious-diseases':
        downloadRecordValue<IInfectiousDiseases>(this.details, this.patientService, url, jsonExtension);
        break;
      case 'medical-problem':
        downloadRecordValue<IMedicalProblem>(this.details, this.patientService, url, jsonExtension);
        break;
      case 'allergy':
        downloadRecordValue<IAdverseEvent>(this.details, this.patientService, url, jsonExtension);
        break;
    }
  }

  openHelpDialog(): any {
    this.updateType();
    this.sharedDataService.showActionMenu = false;
    switch (this.type) {
      case 'vaccination':
        this.helpDialogTitle = 'HELP.VACCINATION.DETAIL.TITLE';
        this.helpDialogBody = 'HELP.VACCINATION.DETAIL.BODY';
        break;
      case 'infectious-diseases':
        this.helpDialogTitle = 'HELP.PAST_ILLNESS.DETAIL.TITLE';
        this.helpDialogBody = 'HELP.PAST_ILLNESS.DETAIL.BODY';
        break;
      case 'medical-problem':
        this.helpDialogTitle = 'HELP.MEDICAL_PROBLEM.DETAIL.TITLE';
        this.helpDialogBody = 'HELP.MEDICAL_PROBLEM.DETAIL.BODY';
        break;
      case 'allergy':
        this.helpDialogTitle = 'HELP.ALLERGY.DETAIL.TITLE';
        this.helpDialogBody = 'HELP.ALLERGY.DETAIL.BODY';
        break;
    }
    this.dialog.openDialog(this.helpDialogTitle, this.helpDialogBody);
  }

  validateRecord(): void {
    this.updateType();
    let copy: any = {
      ...this.details,
    };

    if (this.checkEntry(copy)) {
      copy.author.role = this.sessionInfoService.author.getValue().role;
      switch (this.type) {
        case 'vaccination':
          this.vaccinationService.validate(copy).subscribe(() => window.history.back());
          break;
        case 'infectious-diseases':
          this.illnessService.validate(copy).subscribe(() => window.history.back());
          break;
        case 'medical-problem':
          this.problemService.validate(copy).subscribe(() => window.history.back());
          break;
        case 'allergy':
          this.adverseEventService.validate(copy).subscribe(() => window.history.back());
          break;
      }

      this.sharedDataService.showActionMenu = false;
    }
  }

  previous(): void {
    window.history.back();
  }

  fetchDetails(type: string, id: string): void {
    switch (type) {
      case 'vaccination':
        this.vaccinationService.find(this.itemId).subscribe(details => {
          this.details = this.sharedDataService.storedData['detailedItem'] = details;
        });
        break;
      case 'infectious-diseases':
        this.illnessService.find(id).subscribe(details => {
          this.details = this.sharedDataService.storedData['detailedItem'] = details;
        });
        break;
      case 'medical-problem':
        this.problemService.find(id).subscribe(details => {
          this.details = this.sharedDataService.storedData['detailedItem'] = details;
        });
        break;
      case 'allergy':
        this.adverseEventService.find(id).subscribe(details => {
          this.details = this.sharedDataService.storedData['detailedItem'] = details;
        });
        break;
    }
  }

  private checkEntry(dto: IBaseDTO): boolean {
    let hasFirstName = dto.recorder?.firstName && dto.recorder?.firstName !== '';
    let hasLastName = dto.recorder?.lastName && dto.recorder?.lastName !== '';
    if (dto.recorder && (!hasFirstName || !hasLastName)) {
      this.dialogService.openDialog('GLOBAL.VALIDATION_TITLE', 'GLOBAL.VALIDATION_TEXT');
      return false;
    }

    return true;
  }

  private updateType(): void {
    this.itemId = this.sharedDataService.storedData['detailedItem'].id;
    this.sharedDataService.getSessionStorage();
    this.type = this.router.url.split('/')[1];
    this.details = this.router.url.split('/')[2];
    this.fetchDetails(this.type, this.itemId);
  }
}

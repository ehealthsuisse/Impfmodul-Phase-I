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
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
import { FlexModule } from '@angular/flex-layout';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { DialogService, IBaseDTO, IHumanDTO, IValueDTO, TranslateDirective } from 'src/app/shared';
import { SessionInfoService } from '../../../core/security/session-info.service';
import { BreakPointSensorComponent } from '../../../shared/component/break-point-sensor/break-point-sensor.component';
import { PatientService } from '../../../shared/component/patient/patient.service';
import { deleteRecord, downloadRecordValue } from '../../../shared/function';
import { SharedDataService } from '../../../shared/services/shared-data.service';
import { AdverseEventService } from '../../adverse_event/services/adverse-event.service';
import { BasicImmunizationService } from '../../basic-immunization/service/basic-immunization.service';
import { InfectiousDiseasesService } from '../../infectious_diseases/service/infectious-diseases.service';
import { MedicalProblemService } from '../../medical-problem/service/medical-problem.service';
import { VaccinationService } from '../../vaccination/services/vaccination.service';
import { LaboratorySerologyService } from '../../laboratory-serology/services/laboratory-serology.service';
import { Observable } from 'rxjs';

type RecordService = {
  deleteWithBody(id: string, confidentiality: IValueDTO): unknown;
  find(id?: string): Observable<IBaseDTO>;
  validate(entity: IBaseDTO): Observable<IBaseDTO>;
};

type RecordActionConfig = {
  route: string;
  service: RecordService;
  helpTitle: string;
  helpBody: string;
};

@Component({
  selector: 'vm-details-action',
  standalone: true,
  imports: [CommonModule, FlexModule, MatButtonModule, MatCardModule, MatIconModule, MatTooltipModule, TranslateModule, TranslateDirective],
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
  basicImmunizationService: BasicImmunizationService = inject(BasicImmunizationService);
  adverseEventService: AdverseEventService = inject(AdverseEventService);
  laboratorySerologyService: LaboratorySerologyService = inject(LaboratorySerologyService);
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

  private readonly recordActions: Record<string, RecordActionConfig> = {
    vaccination: {
      route: 'vaccination',
      service: this.vaccinationService,
      helpTitle: 'HELP.VACCINATION.DETAIL.TITLE',
      helpBody: 'HELP.VACCINATION.DETAIL.BODY',
    },
    'infectious-diseases': {
      route: 'infectious-diseases',
      service: this.illnessService,
      helpTitle: 'HELP.PAST_ILLNESS.DETAIL.TITLE',
      helpBody: 'HELP.PAST_ILLNESS.DETAIL.BODY',
    },
    'medical-problem': {
      route: 'medical-problem',
      service: this.problemService,
      helpTitle: 'HELP.MEDICAL_PROBLEM.DETAIL.TITLE',
      helpBody: 'HELP.MEDICAL_PROBLEM.DETAIL.BODY',
    },
    allergy: {
      route: 'allergy',
      service: this.adverseEventService,
      helpTitle: 'HELP.ALLERGY.DETAIL.TITLE',
      helpBody: 'HELP.ALLERGY.DETAIL.BODY',
    },
    'basic-immunization': {
      route: 'basic-immunization',
      service: this.basicImmunizationService,
      helpTitle: 'HELP.BASIC_IMMUNIZATION.DETAIL.TITLE',
      helpBody: 'HELP.BASIC_IMMUNIZATION.DETAIL.BODY',
    },
    'laboratory-serology': {
      route: 'laboratory-serology',
      service: this.laboratorySerologyService,
      helpTitle: 'HELP.LABORATORY_SEROLOGY.DETAIL.TITLE',
      helpBody: 'HELP.LABORATORY_SEROLOGY.DETAIL.BODY',
    },
  };

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
    const action = this.recordActions[this.type];
    if (action) {
      this.router.navigate([action.route, this.itemId, 'edit']);
    }

    this.sharedDataService.showActionMenu = false;
  }

  deleteRecord(): void {
    this.updateType();
    const baseDetails = { ...this.details };
    this.sharedDataService.showActionMenu = false;
    const deleteMessage = this.translateService.instant('GLOBAL.DELETE_CONFIRMATION_MESSAGE');
    const action = this.recordActions[this.type];
    if (action) {
      deleteRecord(this.matDialog, action.service, baseDetails, deleteMessage);
    }
  }

  download(): void {
    this.updateType();
    this.sharedDataService.showActionMenu = false;
    const url = 'data:text/plain;charset=utf-8,' + encodeURIComponent(this.details.json!);
    const jsonExtension = '.json';
    downloadRecordValue<IBaseDTO>(this.details, this.patientService, url, jsonExtension);
  }

  openHelpDialog(): void {
    this.updateType();
    this.sharedDataService.showActionMenu = false;
    const action = this.recordActions[this.type];
    if (action) {
      this.helpDialogTitle = action.helpTitle;
      this.helpDialogBody = action.helpBody;
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
      const action = this.recordActions[this.type];
      if (action) {
        action.service.validate(copy).subscribe(() => window.history.back());
      }

      this.sharedDataService.showActionMenu = false;
    }
  }

  previous(): void {
    window.history.back();
  }

  fetchDetails(type: string, id: string): void {
    const action = this.recordActions[type];
    if (action) {
      action.service.find(id).subscribe(details => {
        this.details = this.sharedDataService.storedData['detailedItem'] = details;
      });
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

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
import { ChangeDetectionStrategy, Component, inject, Input, OnInit } from '@angular/core';
import { FlexModule } from '@angular/flex-layout';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { DialogService, IHumanDTO, TranslateDirective } from 'src/app/shared';
import { SessionInfoService } from '../../../core/security/session-info.service';
import { IAdverseEvent, IInfectiousDiseases, IMedicalProblem, IVaccination } from '../../../model';
import { BreakPointSensorComponent } from '../../../shared/component/break-point-sensor/break-point-sensor.component';
import { deleteRecord, downloadRecordValue } from '../../../shared/function';
import { SharedDataService } from '../../../shared/services/shared-data.service';
import { AdverseEventService } from '../../adverse_event/services/adverse-event.service';
import { InfectiousDiseasesService } from '../../infectious_diseases/service/infectious-diseases.service';
import { MedicalProblemService } from '../../medical-problem/service/medical-problem.service';
import { VaccinationConfirmComponent } from '../../vaccination/helper-components/confirm/vaccination-confirm.component';
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
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  sessionInfoService: SessionInfoService = inject(SessionInfoService);

  @Input() helpDialogTitle!: string;
  @Input() helpDialogBody!: string;
  canValidate!: boolean;
  @Input() canEdit!: boolean;

  get patient(): IHumanDTO {
    return this.sharedDataService.storedData['patient']! ? this.sharedDataService.storedData['patient'] : null;
  }

  ngOnInit(): void {
    this.updateType();

    const role = this.sharedDataService.storedData['role'];
    this.canValidate = (role === 'HCP' || role === 'ASS') && !this.details?.validated;
  }

  editRecord(): void {
    this.updateType();
    if (this.type === 'vaccination') {
      this.router.navigate([`vaccination`, this.details.id, 'edit']);
    }

    if (this.type === 'infectious-diseases') {
      this.router.navigate([`infectious-diseases`, this.details.id, 'edit']);
    }

    if (this.type === 'medical-problem') {
      this.router.navigate([`medical-problem`, this.details.id, 'edit']);
    }

    if (this.type === 'allergy') {
      this.router.navigate([`allergy`, this.details.id, 'edit']);
    }

    this.sharedDataService.showActionMenu = false;
  }

  deleteRecord(): void {
    this.updateType();
    const dialogComponent = VaccinationConfirmComponent;
    const baseDetails = { ...this.details };
    this.sharedDataService.showActionMenu = false;
    switch (this.type) {
      case 'vaccination':
        deleteRecord(this.matDialog, dialogComponent, this.vaccinationService, baseDetails, this.details);
        break;
      case 'infectious-diseases':
        deleteRecord(this.matDialog, dialogComponent, this.illnessService, baseDetails, this.details);
        break;
      case 'medical-problem':
        deleteRecord(this.matDialog, dialogComponent, this.problemService, baseDetails, this.details);
        break;
      case 'allergy':
        deleteRecord(this.matDialog, dialogComponent, this.adverseEventService, baseDetails, this.details);
        break;
    }
  }
  download(): void {
    this.updateType();
    this.sharedDataService.showActionMenu = false;
    switch (this.type) {
      case 'vaccination':
        downloadRecordValue<IVaccination>(this.details, this.patient, this.sessionInfoService);
        break;
      case 'infectious-diseases':
        downloadRecordValue<IInfectiousDiseases>(this.details, this.patient, this.sessionInfoService);
        break;
      case 'medical-problem':
        downloadRecordValue<IMedicalProblem>(this.details, this.patient, this.sessionInfoService);
        break;
      case 'allergy':
        downloadRecordValue<IAdverseEvent>(this.details, this.patient, this.sessionInfoService);
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
    let copy: any = { ...this.details };
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

  previous(): void {
    window.history.back();
  }

  fetchDetails(type: string, id: string): void {
    switch (type) {
      case 'vaccination':
        this.vaccinationService.find(id).subscribe(details => {
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

  private updateType(): void {
    this.sharedDataService.getSessionStorage();
    this.type = this.router.url.split('/')[1];
    this.details = this.router.url.split('/')[2];
    this.fetchDetails(this.type, this.details);
  }
}

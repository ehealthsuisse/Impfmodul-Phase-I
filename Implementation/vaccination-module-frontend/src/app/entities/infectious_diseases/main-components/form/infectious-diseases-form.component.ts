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
import { AfterViewInit, Component, inject, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSelect } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, Observable, ReplaySubject } from 'rxjs';
import { IInfectiousDiseases } from '../../../../model';
import { FormOptionsService, IValueDTO } from '../../../../shared';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';
import { filterDropdownList, initializeActionData, openSnackBar, setDropDownInitialValue } from '../../../../shared/function';
import { FilterPipePipe } from '../../../../shared/pipes/filter-pipe.pipe';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedComponentModule } from '../../../../shared/shared-component.module';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { InfectiousDiseasesConfirmComponent } from '../../helper-components/confirm/infectious-diseases-confirm.component';
import { InfectiousDiseasesFormGroup, InfectiousDiseasesFormService } from '../../service/infectious-diseases-form.service';
import { InfectiousDiseasesService } from '../../service/infectious-diseases.service';
import { ReusableDateFieldComponent } from '../../../../shared/component/resuable-fields/reusable-date-field/reusable-date-field.component';
import { ReusableRecorderFieldComponent } from '../../../../shared/component/resuable-fields/reusable-recorder-field/reusable-recorder-field.component';
import { ConfidentialityService } from '../../../../shared/component/confidentiality/confidentiality.service';
import { SessionInfoService } from '../../../../core/security/session-info.service';

@Component({
  standalone: true,
  selector: 'vm-infectious-diseases-update',
  templateUrl: './infectious-diseases-form.component.html',
  styleUrls: ['./infectious-diseases-form.component.scss'],
  imports: [SharedLibsModule, SharedComponentModule, FilterPipePipe, ReusableDateFieldComponent, ReusableRecorderFieldComponent],
})
export class InfectiousDiseasesFormComponent extends BreakPointSensorComponent implements OnInit, AfterViewInit {
  illnessesFilteredList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  @ViewChild('singleSelect', { static: true }) singleSelect!: MatSelect;
  illnessesFilterControl: FormControl = new FormControl();

  isSaving = false;
  illnesses: IInfectiousDiseases | null = null;
  editForm: InfectiousDiseasesFormGroup = inject(InfectiousDiseasesFormService).createInfectiousDiseasesFormGroup();
  formOptionsService: FormOptionsService = inject(FormOptionsService);
  illness: IInfectiousDiseases | null = null;
  router = inject(Router);
  formOptions: Map<string, IValueDTO[]> = new Map<string, IValueDTO[]>();
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  illnessService: InfectiousDiseasesService = inject(InfectiousDiseasesService);
  helpDialogTitle = 'HELP.PAST_ILLNESS.DETAIL.TITLE';
  helpDialogBody = 'HELP.PAST_ILLNESS.DETAIL.BODY';
  sharedDataService: SharedDataService = inject(SharedDataService);
  searchControl = new FormControl();
  sessionInfoService: SessionInfoService = inject(SessionInfoService);

  confidentialityService: ConfidentialityService = inject(ConfidentialityService);

  private illnessesFormService: InfectiousDiseasesFormService = inject(InfectiousDiseasesFormService);
  private matDialog: MatDialog = inject(MatDialog);

  ngOnInit(): void {
    this.displayMenu(false, false);
    let id = this.activatedRoute.snapshot.params['id'];
    this.illnessService.find(id).subscribe(illness => {
      if (illness) {
        this.illness = illness;
        this.updateForm(this.illness);
      } else {
        this.illnessService.query().subscribe({
          next: list => {
            this.illness = list.find(filteredIllness => filteredIllness.id === id)!;
            this.updateForm(this.illness);
          },
        });
      }
    });

    this.illnessesFilterControl.valueChanges.subscribe(() => {
      filterDropdownList(this.formOptions.get('conditionCode')!, this.illnessesFilteredList, this.illnessesFilterControl);
    });
    initializeActionData('', this.sharedDataService);
    this.processFormOptions();
  }

  ngAfterViewInit(): void {
    setDropDownInitialValue(this.illnessesFilteredList, this.singleSelect);
  }

  save(): void {
    if (this.editForm.value.commentMessage) {
      const commentObj = {
        text: this.editForm.value.commentMessage,
        author: 'will be added by the system',
      };
      this.editForm.value.comments = Object.assign([], this.editForm.value.comments);
      this.editForm.value.comments!.push(commentObj);
    }
    const illness = { ...this.illness, ...this.illnessesFormService.getInfectiousDiseases(this.editForm) };
    illness.verificationStatus = this.formOptionsService.getOption('conditionVerificationStatus', 'unconfirmed');
    illness.clinicalStatus = this.formOptionsService.getOption('conditionClinicalStatus', 'resolved');
    illness.illnessCode = illness.code;
    /* eslint-disable-next-line security/detect-non-literal-fs-filename -- Safe as no value holds user input */
    this.matDialog
      .open(InfectiousDiseasesConfirmComponent, {
        width: '60vw',
        data: { value: { ...illness }, button: { save: 'buttons.SAVE', saveAndStay: 'buttons.SAVE_AND_STAY' } },
        disableClose: true,
      })
      .afterClosed()
      .subscribe({
        next: (result: { action?: string } = {}) => {
          illness.confidentiality = this.confidentialityService.confidentialityStatus;
          if (result.action === 'SAVE') {
            if (illness.id) {
              this.subscribeToSaveResponse(this.illnessService.update(illness), true, true);
            } else {
              this.subscribeToSaveResponse(this.illnessService.create(illness), true, false);
            }
          }
          if (result.action === 'SAVE_AND_STAY') {
            illness.confidentiality = this.illnessService.confidentialityStatus;
            this.subscribeToSaveResponse(this.illnessService.create(illness), false, false);
          }
        },
      });
  }

  private processFormOptions(): void {
    this.formOptionsService.getAllOptions().subscribe({
      next: options =>
        options.map(option => {
          this.formOptions.set(option.name, option.entries!);
          this.illnessesFilteredList.next(this.formOptions.get('conditionCode')!);
        }),
    });
  }

  private onSaveSuccess(navigate: boolean, isUpdate: boolean): void {
    if (isUpdate) {
      this.router.navigate(['/infectious-diseases']);
      return;
    }

    if (navigate) {
      window.history.back();
    }
    if (!navigate) {
      openSnackBar(this.translateService, this.snackBar, 'HELP.PAST_ILLNESS.SAVE_AND_STAY.BODY');
    }
    this.isSaving = false;
  }

  private updateForm(illnesses: IInfectiousDiseases): void {
    this.illnesses = illnesses;
    this.illnessesFormService.resetForm(this.editForm, illnesses);
  }

  private onSaveFinalize(): void {
    this.isSaving = false;
  }

  private subscribeToSaveResponse(result: Observable<IInfectiousDiseases>, navigate: boolean, isUpdate: boolean): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(navigate, isUpdate),
    });
  }
}

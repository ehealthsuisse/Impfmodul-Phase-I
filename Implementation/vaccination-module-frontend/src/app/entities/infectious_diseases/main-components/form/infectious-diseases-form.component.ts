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
import { AfterViewInit, Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSelect } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, Observable, ReplaySubject, Subject } from 'rxjs';
import { SessionInfoService } from '../../../../core/security/session-info.service';
import { IInfectiousDiseases } from '../../../../model';
import { FormOptionsService, IValueDTO } from '../../../../shared';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';
import { ConfidentialityService } from '../../../../shared/services/confidentiality.service';
import { ReusableDateFieldComponent } from '../../../../shared/component/resuable-fields/reusable-date-field/reusable-date-field.component';
import { ReusableRecorderFieldComponent } from '../../../../shared/component/resuable-fields/reusable-recorder-field/reusable-recorder-field.component';
import { ReusableSelectFieldWithSearchComponent } from '../../../../shared/component/resuable-fields/reusable-select-field-with-search/reusable-select-field-with-search.component';
import { buildComment, initializeActionData, openSnackBar, routecall, setDropDownInitialValue } from '../../../../shared/function';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedComponentModule } from '../../../../shared/shared-component.module';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { InfectiousDiseasesConfirmComponent } from '../../helper-components/confirm/infectious-diseases-confirm.component';
import { InfectiousDiseasesFormGroup, InfectiousDiseasesFormService } from '../../service/infectious-diseases-form.service';
import { InfectiousDiseasesService } from '../../service/infectious-diseases.service';
import { takeUntil } from 'rxjs/operators';
import { FormGroupDirective } from '@angular/forms';

@Component({
  selector: 'vm-infectious-diseases-update',
  standalone: true,
  templateUrl: './infectious-diseases-form.component.html',
  styleUrls: ['./infectious-diseases-form.component.scss'],
  imports: [
    SharedLibsModule,
    SharedComponentModule,
    ReusableDateFieldComponent,
    ReusableRecorderFieldComponent,
    ReusableSelectFieldWithSearchComponent,
  ],
})
export class InfectiousDiseasesFormComponent extends BreakPointSensorComponent implements OnInit, AfterViewInit, OnDestroy {
  illnessesFilteredList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  confidentialityList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  @ViewChild('singleSelect', { static: true }) singleSelect!: MatSelect;
  @ViewChild('formGroupDirective') formGroupDirective!: FormGroupDirective;
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
  sessionInfoService: SessionInfoService = inject(SessionInfoService);
  confidentialityService: ConfidentialityService = inject(ConfidentialityService);
  commentMessage: string = '';

  private illnessesFormService: InfectiousDiseasesFormService = inject(InfectiousDiseasesFormService);
  private matDialog: MatDialog = inject(MatDialog);
  private destroy$: Subject<void> = new Subject<void>();

  ngOnInit(): void {
    this.displayMenu(false, false);
    let id = this.activatedRoute.snapshot.params['id'];
    let role: string = this.sharedDataService.storedData['role']!;
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

    initializeActionData('', this.sharedDataService);
    this.processFormOptions();
    this.confidentialityService.loadConfidentialityOptionsWithDefaultSelection(role, this.confidentialityList, this.editForm);
  }

  ngAfterViewInit(): void {
    setDropDownInitialValue(this.illnessesFilteredList, this.singleSelect);
    // When the begin date changes, re-validate the 'end' field
    this.editForm
      .get('begin')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.editForm.get('end')?.updateValueAndValidity();
        this.editForm.get('recordedDate')?.updateValueAndValidity();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  save(): void {
    this.editForm.value.comment = buildComment(this.commentMessage);

    const illness = { ...this.illness, ...this.illnessesFormService.getInfectiousDiseases(this.editForm) };
    illness.verificationStatus = this.formOptionsService.getOption(
      'conditionVerificationStatus',
      this.sessionInfoService.canValidate() ? 'confirmed' : 'unconfirmed'
    );
    illness.clinicalStatus = this.formOptionsService.getOption('conditionClinicalStatus', 'resolved');
    illness.illnessCode = illness.code;

    this.matDialog
      .open(InfectiousDiseasesConfirmComponent, {
        width: '60vw',
        data: { value: { ...illness }, button: { save: 'buttons.SAVE', saveAndStay: 'buttons.SAVE_AND_STAY' } },
        disableClose: true,
      })
      .afterClosed()
      .subscribe({
        next: (result: { action?: string } = {}) => {
          if (result.action === 'SAVE') {
            if (illness.id) {
              this.subscribeToSaveResponse(this.illnessService.update(illness), true);
            } else {
              this.subscribeToSaveResponse(this.illnessService.create(illness), true);
            }
          }
          if (result.action === 'SAVE_AND_STAY') {
            illness.confidentiality = this.illnessService.confidentialityStatus;
            this.subscribeToSaveResponse(this.illnessService.create(illness), false);
            this.formGroupDirective.resetForm();
            this.illnessesFormService.resetMandatoryFields(this.editForm);
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

  private onSaveSuccess(navigate: boolean): void {
    if (navigate) {
      routecall(this.router, this.sessionInfoService, '/infectious-diseases');
    } else {
      openSnackBar(this.translateService, this.snackBar, 'HELP.PAST_ILLNESS.SAVE_AND_STAY.BODY');
    }
  }

  private updateForm(illnesses: IInfectiousDiseases): void {
    this.illnesses = illnesses;
    this.illnessesFormService.resetForm(this.editForm, illnesses);
  }

  private onSaveFinalize(): void {
    this.isSaving = false;
  }

  private subscribeToSaveResponse(result: Observable<IInfectiousDiseases>, navigate: boolean): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(navigate),
    });
  }
}

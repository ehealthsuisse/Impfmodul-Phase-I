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
import { IMedicalProblem } from '../../../../model';
import { FormOptionsService, IValueDTO } from '../../../../shared';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';
import { ConfidentialityService } from '../../../../shared/services/confidentiality.service';
import { ReusableDateFieldComponent } from '../../../../shared/component/resuable-fields/reusable-date-field/reusable-date-field.component';
import { ReusableRecorderFieldComponent } from '../../../../shared/component/resuable-fields/reusable-recorder-field/reusable-recorder-field.component';
import { ReusableSelectFieldWithSearchComponent } from '../../../../shared/component/resuable-fields/reusable-select-field-with-search/reusable-select-field-with-search.component';
import { ReusableSelectFieldComponent } from '../../../../shared/component/resuable-fields/reusable-select-field/reusable-select-field.component';
import { buildComment, initializeActionData, openSnackBar, routecall, setDropDownInitialValue } from '../../../../shared/function';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedComponentModule } from '../../../../shared/shared-component.module';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { MedicalProblemConfirmComponent } from '../../helper-components/confirm/medical-problem-confirm.component';
import { MedicalProblemFormService, ProblemFormGroup } from '../../service/medical-problem-form.service';
import { MedicalProblemService } from '../../service/medical-problem.service';
import { takeUntil } from 'rxjs/operators';
import { FormGroupDirective } from '@angular/forms';

@Component({
  selector: 'vm-problem-update',
  standalone: true,
  templateUrl: './medical-problem-form.component.html',
  styleUrls: ['./medical-problem-form.component.scss'],
  imports: [
    SharedLibsModule,
    SharedComponentModule,
    ReusableDateFieldComponent,
    ReusableRecorderFieldComponent,
    ReusableSelectFieldComponent,
    ReusableSelectFieldWithSearchComponent,
  ],
})
export class MedicalProblemFormComponent extends BreakPointSensorComponent implements OnInit, AfterViewInit, OnDestroy {
  problemFilteredList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  confidentialityList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  problemStatus: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  @ViewChild('singleSelect', { static: true }) singleSelect!: MatSelect;
  @ViewChild('clinicalStatus', { static: true }) clinicalStatus!: MatSelect;
  @ViewChild('formGroupDirective') formGroupDirective!: FormGroupDirective;
  isSaving = false;
  problems: IMedicalProblem | null = null;
  editForm: ProblemFormGroup = inject(MedicalProblemFormService).createProblemFormGroup();
  formOptionsService: FormOptionsService = inject(FormOptionsService);
  problem: IMedicalProblem | null = null;
  router = inject(Router);
  formOptions: Map<string, IValueDTO[]> = new Map<string, IValueDTO[]>();
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  problemService: MedicalProblemService = inject(MedicalProblemService);
  helpDialogTitle = 'HELP.MEDICAL_PROBLEM.DETAIL.TITLE';
  helpDialogBody = 'HELP.MEDICAL_PROBLEM.DETAIL.BODY';
  sharedDataService: SharedDataService = inject(SharedDataService);
  sessionInfoService: SessionInfoService = inject(SessionInfoService);
  confidentialityService: ConfidentialityService = inject(ConfidentialityService);
  commentMessage: string = '';
  private problemFormService: MedicalProblemFormService = inject(MedicalProblemFormService);
  private matDialog: MatDialog = inject(MatDialog);
  private destroy$: Subject<void> = new Subject<void>();

  ngOnInit(): void {
    this.displayMenu(false, false);
    initializeActionData('', this.sharedDataService);
    let id = this.activatedRoute.snapshot.params['id'];
    let role: string = this.sharedDataService.storedData['role']!;
    this.problemService.find(id).subscribe(problem => {
      if (problem) {
        this.problem = problem;
        this.updateForm(this.problem);
      } else {
        this.problemService.query().subscribe({
          next: list => {
            this.problem = list.find(filteredProblem => filteredProblem.id === id)!;
            this.updateForm(this.problem);
          },
        });
      }
    });

    this.processFormOptions();
    this.confidentialityService.loadConfidentialityOptionsWithDefaultSelection(role, this.confidentialityList, this.editForm);
  }

  ngAfterViewInit(): void {
    setDropDownInitialValue(this.problemStatus, this.clinicalStatus);
    setDropDownInitialValue(this.problemFilteredList, this.singleSelect);
    // When the begin date changes, re-validate the 'end' field
    this.editForm
      .get('begin')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.editForm.get('end')?.updateValueAndValidity();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  save(): void {
    this.editForm.value.comment = buildComment(this.commentMessage);

    const problem = { ...this.problem, ...this.problemFormService.getProblem(this.editForm) };
    problem.verificationStatus = this.formOptionsService.getOption(
      'conditionVerificationStatus',
      this.sessionInfoService.canValidate() ? 'confirmed' : 'unconfirmed'
    );

    this.matDialog
      .open(MedicalProblemConfirmComponent, {
        width: '60vw',
        data: { value: { ...problem }, button: { save: 'buttons.SAVE', saveAndStay: 'buttons.SAVE_AND_STAY' } },
        disableClose: true,
      })
      .afterClosed()
      .subscribe({
        next: (result: { action?: string } = {}) => {
          if (result.action === 'SAVE') {
            if (problem.id) {
              this.subscribeToSaveResponse(this.problemService.update(problem), true);
            } else {
              this.subscribeToSaveResponse(this.problemService.create(problem), true);
            }
          }
          if (result.action === 'SAVE_AND_STAY') {
            this.subscribeToSaveResponse(this.problemService.create(problem), false);
            this.formGroupDirective.resetForm();
            this.problemFormService.resetMandatoryFields(this.editForm);
          }
        },
      });
  }

  private processFormOptions(): void {
    this.formOptionsService.getAllOptions().subscribe({
      next: options => {
        options.map(option => {
          this.formOptions.set(
            option.name,
            option.entries!.filter(entry => entry.allowDisplay)
          );
        });
        this.problemFilteredList.next(this.formOptions.get('medicalProblemCode')!);
        this.problemStatus.next(this.formOptions.get('conditionClinicalStatus')!);
      },
    });
  }

  private onSaveSuccess(navigate: boolean): void {
    if (navigate) {
      routecall(this.router, this.sessionInfoService, '/medical-problem');
    } else {
      openSnackBar(this.translateService, this.snackBar, 'HELP.MEDICAL_PROBLEM.SAVE_AND_STAY.BODY');
    }
  }

  private updateForm(problems: IMedicalProblem): void {
    this.problems = problems;
    this.problemFormService.resetForm(this.editForm, problems);
  }

  private onSaveFinalize(): void {
    this.isSaving = false;
  }

  private subscribeToSaveResponse(result: Observable<IMedicalProblem>, navigate: boolean): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(navigate),
    });
  }
}

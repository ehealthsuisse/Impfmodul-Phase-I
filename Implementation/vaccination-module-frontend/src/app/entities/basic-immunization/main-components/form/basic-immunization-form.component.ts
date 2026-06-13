/**
 * Copyright (c) 2026 eHealth Suisse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
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
import { IBasicImmunization } from '../../../../model';
import { FormOptionsService, IValueDTO } from '../../../../shared';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';
import { ConfidentialityService } from '../../../../shared/services/confidentiality.service';
import { ReusableDateFieldComponent } from '../../../../shared/component/resuable-fields/reusable-date-field/reusable-date-field.component';
import { ReusableSelectFieldComponent } from '../../../../shared/component/resuable-fields/reusable-select-field/reusable-select-field.component';
import { ReusableSelectFieldWithSearchComponent } from '../../../../shared/component/resuable-fields/reusable-select-field-with-search/reusable-select-field-with-search.component';
import { buildComment, initializeActionData, openSnackBar, routecall, setDropDownInitialValue } from '../../../../shared/function';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedComponentModule } from '../../../../shared/shared-component.module';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { BasicImmunizationConfirmComponent } from '../../helper-components/confirm/basic-immunization-confirm.component';
import { BasicImmunizationFormService, BasicImmunizationFormGroup } from '../../service/basic-immunization-form.service';
import { BasicImmunizationService } from '../../service/basic-immunization.service';
import { FormGroupDirective } from '@angular/forms';

@Component({
  selector: 'vm-basic-immunization-update',
  templateUrl: './basic-immunization-form.component.html',
  styleUrls: ['./basic-immunization-form.component.scss'],
  imports: [
    SharedLibsModule,
    SharedComponentModule,
    ReusableDateFieldComponent,
    ReusableSelectFieldComponent,
    ReusableSelectFieldWithSearchComponent,
  ],
})
export class BasicImmunizationFormComponent extends BreakPointSensorComponent implements OnInit, AfterViewInit, OnDestroy {
  basicImmunizationFilteredList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  confidentialityList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  @ViewChild('singleSelect', { static: true }) singleSelect!: MatSelect;
  @ViewChild('formGroupDirective') formGroupDirective!: FormGroupDirective;
  isSaving = false;
  basicImmunizations: IBasicImmunization | null = null;
  editForm: BasicImmunizationFormGroup = inject(BasicImmunizationFormService).createBasicImmunizationFormGroup();
  formOptionsService: FormOptionsService = inject(FormOptionsService);
  basicImmunization: IBasicImmunization | null = null;
  router = inject(Router);
  formOptions: Map<string, IValueDTO[]> = new Map<string, IValueDTO[]>();
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  basicImmunizationService: BasicImmunizationService = inject(BasicImmunizationService);
  helpDialogTitle = 'HELP.BASIC_IMMUNIZATION.DETAIL.TITLE';
  helpDialogBody = 'HELP.BASIC_IMMUNIZATION.DETAIL.BODY';
  sharedDataService: SharedDataService = inject(SharedDataService);
  sessionInfoService: SessionInfoService = inject(SessionInfoService);
  confidentialityService: ConfidentialityService = inject(ConfidentialityService);
  commentMessage: string = '';
  private basicImmunizationFormService: BasicImmunizationFormService = inject(BasicImmunizationFormService);
  private matDialog: MatDialog = inject(MatDialog);
  private destroy$: Subject<void> = new Subject<void>();

  ngOnInit(): void {
    this.displayMenu(false, false);
    initializeActionData('', this.sharedDataService);
    this.loadBasicImmunizationData();
    this.initializeFormOptions();
  }

  ngAfterViewInit(): void {
    setDropDownInitialValue(this.basicImmunizationFilteredList, this.singleSelect);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  save(): void {
    this.editForm.value.comment = buildComment(this.commentMessage);

    const basicImmunization = { ...this.basicImmunization, ...this.basicImmunizationFormService.getBasicImmunization(this.editForm) };
    basicImmunization.verificationStatus = this.formOptionsService.getOption(
      'conditionVerificationStatus',
      this.sessionInfoService.canValidate() ? 'confirmed' : 'unconfirmed'
    );
    basicImmunization.category = this.formOptionsService.getOption('basicImmunizationCategory', 'encounter-diagnosis');

    this.matDialog
      .open(BasicImmunizationConfirmComponent, {
        width: '60vw',
        data: { value: { ...basicImmunization }, button: { save: 'buttons.SAVE', saveAndStay: 'buttons.SAVE_AND_STAY' } },
        disableClose: true,
      })
      .afterClosed()
      .subscribe({
        next: (result: { action?: string } = {}) => {
          if (result.action === 'SAVE') {
            if (basicImmunization.id) {
              this.subscribeToSaveResponse(this.basicImmunizationService.update(basicImmunization), true);
            } else {
              this.subscribeToSaveResponse(this.basicImmunizationService.create(basicImmunization), true);
            }
          }
          if (result.action === 'SAVE_AND_STAY') {
            this.subscribeToSaveResponse(this.basicImmunizationService.create(basicImmunization), false);
            this.formGroupDirective.resetForm();
            this.basicImmunizationFormService.resetMandatoryFields(this.editForm);
          }
        },
      });
  }

  private initializeFormOptions(): void {
    const role: string = this.sharedDataService.storedData['role']!;
    this.processFormOptions();
    this.confidentialityService.loadConfidentialityOptionsWithDefaultSelection(role, this.confidentialityList, this.editForm);
  }

  private loadBasicImmunizationData(): void {
    const id = this.activatedRoute.snapshot.params['id'];
    this.basicImmunizationService.find(id).subscribe(basicImmunization => {
      if (basicImmunization) {
        this.basicImmunization = basicImmunization;
        this.updateForm(this.basicImmunization);
      } else {
        this.basicImmunizationService.query().subscribe({
          next: list => {
            this.basicImmunization = list.find(filtered => filtered.id === id)!;
            this.updateForm(this.basicImmunization);
          },
        });
      }
      this.commentMessage = this.editForm.get('comment')?.value?.text || '';
    });
  }

  private onSaveFinalize(): void {
    this.isSaving = false;
  }

  private onSaveSuccess(navigate: boolean): void {
    if (navigate) {
      routecall(this.router, this.sessionInfoService, '/basic-immunization');
    } else {
      openSnackBar(this.translateService, this.snackBar, 'HELP.BASIC_IMMUNIZATION.SAVE_AND_STAY.BODY');
    }
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
        this.basicImmunizationFilteredList.next(this.formOptions.get('basicImmunizationCode')!);
      },
    });
  }

  private subscribeToSaveResponse(result: Observable<IBasicImmunization>, navigate: boolean): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(navigate),
    });
  }

  private updateForm(basicImmunization: IBasicImmunization): void {
    this.basicImmunizations = basicImmunization;
    this.basicImmunizationFormService.resetForm(this.editForm, basicImmunization);
  }
}

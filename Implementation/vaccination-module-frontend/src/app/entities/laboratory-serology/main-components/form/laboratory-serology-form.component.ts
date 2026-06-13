/**
 * Copyright (c) 2026 eHealth Suisse
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
import { FormGroupDirective } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSelect } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, map, Observable, ReplaySubject, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SessionInfoService } from '../../../../core/security/session-info.service';
import { ILaboratorySerology } from '../../../../model';
import { FormOptionsService, IValueDTO } from '../../../../shared';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';
import { ReusableDateFieldComponent } from '../../../../shared/component/resuable-fields/reusable-date-field/reusable-date-field.component';
import { ReusableRecorderFieldComponent } from '../../../../shared/component/resuable-fields/reusable-recorder-field/reusable-recorder-field.component';
import { ReusableSelectFieldComponent } from '../../../../shared/component/resuable-fields/reusable-select-field/reusable-select-field.component';
import { ReusableSelectFieldWithSearchComponent } from '../../../../shared/component/resuable-fields/reusable-select-field-with-search/reusable-select-field-with-search.component';
import { buildComment, initializeActionData, openSnackBar, routecall, setDropDownInitialValue } from '../../../../shared/function';
import { ConfidentialityService } from '../../../../shared/services/confidentiality.service';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedComponentModule } from '../../../../shared/shared-component.module';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { LaboratorySerologyConfirmComponent } from '../../helper-components/confirm/laboratory-serology-confirm.component';
import { LaboratorySerologyFormGroup, LaboratorySerologyFormService } from '../../services/laboratory-serology-form.service';
import { LaboratorySerologyService } from '../../services/laboratory-serology.service';
import { VALIDATION_CODES } from '../../../../shared/constants/global.constants';

@Component({
  selector: 'vm-laboratory-serology-form',
  standalone: true,
  templateUrl: './laboratory-serology-form.component.html',
  styleUrls: ['./laboratory-serology-form.component.scss'],
  imports: [
    SharedLibsModule,
    SharedComponentModule,
    ReusableDateFieldComponent,
    ReusableRecorderFieldComponent,
    ReusableSelectFieldComponent,
    ReusableSelectFieldWithSearchComponent,
  ],
})
export class LaboratorySerologyFormComponent extends BreakPointSensorComponent implements AfterViewInit, OnDestroy, OnInit {
  filteredCodes: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  confidentialityList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  sharedDataService: SharedDataService = inject(SharedDataService);
  @ViewChild('codeSelect', { static: true }) codeSelect!: MatSelect;
  @ViewChild('formGroupDirective') formGroupDirective!: FormGroupDirective;
  isSaving = false;
  editForm: LaboratorySerologyFormGroup = inject(LaboratorySerologyFormService).createFormGroup();
  formOptionsService: FormOptionsService = inject(FormOptionsService);
  laboratorySerology: ILaboratorySerology | null = null;
  router = inject(Router);
  formOptions: Map<string, IValueDTO[]> = new Map<string, IValueDTO[]>();
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  laboratorySerologyService: LaboratorySerologyService = inject(LaboratorySerologyService);
  helpDialogTitle = 'HELP.LABORATORY_SEROLOGY.DETAIL.TITLE';
  helpDialogBody = 'HELP.LABORATORY_SEROLOGY.DETAIL.BODY';
  sessionInfoService: SessionInfoService = inject(SessionInfoService);
  confidentialityService: ConfidentialityService = inject(ConfidentialityService);
  commentMessage: string = '';

  private formService: LaboratorySerologyFormService = inject(LaboratorySerologyFormService);
  private matDialog: MatDialog = inject(MatDialog);
  private observationUnitsByCode: Record<string, IValueDTO> = {};
  private unsubscribe$ = new Subject<void>();

  ngOnInit(): void {
    this.displayMenu(false, false);
    initializeActionData('', this.sharedDataService);
    this.loadLaboratorySerologyData();
    this.initializeFormOptions();
    this.listenToLaboratorySerologyCodeChanges();
  }

  ngAfterViewInit(): void {
    setDropDownInitialValue(this.filteredCodes, this.codeSelect);
  }

  get unitDisplayName(): string {
    return this.editForm.controls.valueUnit.value?.name ?? '';
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  save(): void {
    this.editForm.value.comment = buildComment(this.commentMessage);

    const laboratorySerology = { ...this.laboratorySerology, ...this.formService.getLaboratorySerology(this.editForm) };
    laboratorySerology.status = this.getFinalObservationStatus();
    laboratorySerology.verificationStatus = this.formOptionsService.getOption(
      'observationVerificationStatus',
      this.sessionInfoService.canValidate() ? VALIDATION_CODES.CAN_VALIDATE : VALIDATION_CODES.CANNOT_VALIDATE
    );

    this.matDialog
      .open(LaboratorySerologyConfirmComponent, {
        width: '60vw',
        data: { value: { ...laboratorySerology }, button: { save: 'buttons.SAVE', saveAndStay: 'buttons.SAVE_AND_STAY' } },
        disableClose: true,
      })
      .afterClosed()
      .subscribe((result: { action?: string } = {}) => {
        if (result.action === 'SAVE') {
          if (laboratorySerology.id) {
            this.subscribeToSaveResponse(this.laboratorySerologyService.update(laboratorySerology), true);
          } else {
            this.subscribeToSaveResponse(this.laboratorySerologyService.create(laboratorySerology), true);
          }
        }
        if (result.action === 'SAVE_AND_STAY') {
          this.subscribeToSaveResponse(this.laboratorySerologyService.create(laboratorySerology), false);
          this.formGroupDirective.resetForm();
          this.formService.resetMandatoryFields(this.editForm);
          this.editForm.controls.status.setValue(this.getFinalObservationStatus());
        }
      });
  }

  private initializeFormOptions(): void {
    const role: string = this.sharedDataService.storedData['role']!;
    this.processFormOptions();
    this.confidentialityService.loadConfidentialityOptionsWithDefaultSelection(role, this.confidentialityList, this.editForm);
  }

  private loadLaboratorySerologyData(): void {
    const id = this.activatedRoute.snapshot.params['id'];
    if (!id) {
      this.formService.resetMandatoryFields(this.editForm);
      return;
    }

    this.laboratorySerologyService.find(id).subscribe(record => {
      if (record) {
        this.laboratorySerology = record;
        this.updateForm(this.laboratorySerology);
      } else {
        this.laboratorySerologyService.query().subscribe({
          next: list => {
            this.laboratorySerology = list.find(filteredRecord => filteredRecord.id === id)!;
            this.updateForm(this.laboratorySerology);
          },
        });
      }
      this.commentMessage = this.editForm.get('comment')?.value?.text || '';
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
        this.filteredCodes.next(this.formOptions.get('observationCode') || []);
        this.editForm.controls.status.setValue(this.getFinalObservationStatus());
      },
    });
    this.formOptionsService
      .getObservationCodesToUnits()
      .pipe(
        map(mappings =>
          mappings.reduce(
            (acc, mapping) => ({
              ...acc,
              [mapping.observationCode.code]: mapping.unit,
            }),
            {} as Record<string, IValueDTO>
          )
        ),
        takeUntil(this.unsubscribe$)
      )
      .subscribe(unitsByCode => {
        this.observationUnitsByCode = unitsByCode;
        this.updateUnitForCode(this.editForm.controls.code.value);
      });
  }

  private updateForm(record: ILaboratorySerology): void {
    this.laboratorySerology = record;
    this.formService.resetForm(this.editForm, record);
    this.updateUnitForCode(this.editForm.controls.code.value);
  }

  private listenToLaboratorySerologyCodeChanges(): void {
    this.editForm.controls.code.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(code => this.updateUnitForCode(code));
  }

  private updateUnitForCode(code: IValueDTO | string | null): void {
    const observationCode = typeof code === 'string' ? code : code?.code;
    const unit = observationCode ? this.observationUnitsByCode[observationCode] ?? null : null;
    this.editForm.controls.valueUnit.setValue(unit);
  }

  private getFinalObservationStatus(): IValueDTO {
    return this.formOptionsService.getOption('observationStatus', 'final');
  }

  private subscribeToSaveResponse(result: Observable<ILaboratorySerology>, navigate: boolean): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(navigate),
    });
  }

  private onSaveSuccess(navigate: boolean): void {
    if (navigate) {
      routecall(this.router, this.sessionInfoService, '/laboratory-serology');
    } else {
      openSnackBar(this.translateService, this.snackBar, 'HELP.LABORATORY_SEROLOGY.SAVE_AND_STAY.BODY');
    }
  }

  private onSaveFinalize(): void {
    this.isSaving = false;
  }
}

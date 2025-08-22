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
import { AfterViewInit, Component, inject, OnDestroy, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatDialog } from '@angular/material/dialog';
import { MatSelect } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import _ from 'lodash';
import { finalize, map, Observable, ReplaySubject, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SessionInfoService } from '../../../../core/security/session-info.service';
import { IVaccination } from '../../../../model';
import { CommentComponent, CommonCardFooterComponent, FormOptionsService, IValueDTO } from '../../../../shared';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';
import { ConfidentialityService } from '../../../../shared/services/confidentiality.service';
import { ReusableDateFieldComponent } from '../../../../shared/component/resuable-fields/reusable-date-field/reusable-date-field.component';
import { ReusableSelectFieldWithSearchComponent } from '../../../../shared/component/resuable-fields/reusable-select-field-with-search/reusable-select-field-with-search.component';
import { buildComment, initializeActionData, openSnackBar, routecall, setDropDownInitialValue } from '../../../../shared/function';
import { VaccinePredefinedDiseases } from '../../../../shared/interfaces/vaccination-with-disease.interface';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { VaccinationConfirmComponent } from '../../helper-components/confirm/vaccination-confirm.component';
import { VaccinationFormGroup, VaccinationFormService } from '../../services/vaccination-form.service';
import { VaccinationService } from '../../services/vaccination.service';
import { ChipsHandler } from './chips-handler';
import { SpinnerService } from '../../../../shared/services/spinner.service';
import { FormGroupDirective } from '@angular/forms';

@Component({
  selector: 'vm-vaccination-form',
  standalone: true,
  templateUrl: './vaccination-form.component.html',
  styleUrls: ['./vaccination-form.component.scss'],
  encapsulation: ViewEncapsulation.None,
  imports: [
    SharedLibsModule,
    CommonCardFooterComponent,
    CommentComponent,
    ReusableDateFieldComponent,
    ReusableSelectFieldWithSearchComponent,
  ],
})
export class VaccinationFormComponent extends BreakPointSensorComponent implements OnInit, AfterViewInit, OnDestroy {
  vaccinationFilteredList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  confidentialityList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  reasons: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  diseases: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);

  @ViewChild('singleSelect', { static: true }) singleSelect!: MatSelect;
  @ViewChild('status', { static: true }) status!: MatSelect;
  @ViewChild('reason', { static: true }) reason!: MatSelect;
  @ViewChild('diseasesChipsList', { static: true }) diseasesList!: MatSelect;
  @ViewChild('formGroupDirective') formGroupDirective!: FormGroupDirective;

  isSaving = false;
  editForm: VaccinationFormGroup = inject(VaccinationFormService).createVaccinationFormGroup();
  formOptionsService: FormOptionsService = inject(FormOptionsService);
  vaccination: IVaccination | null = null;
  diseasesChips = new ChipsHandler('disease', this.translateService);
  router = inject(Router);
  formOptions: Map<string, IValueDTO[]> = new Map<string, IValueDTO[]>();
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  vaccinationService: VaccinationService = inject(VaccinationService);
  sharedDataService: SharedDataService = inject(SharedDataService);
  helpDialogTitle = 'HELP.VACCINATION.DETAIL.TITLE';
  helpDialogBody = 'HELP.VACCINATION.DETAIL.BODY';
  sessionInfoService: SessionInfoService = inject(SessionInfoService);
  confidentialityService: ConfidentialityService = inject(ConfidentialityService);
  spinnerService: SpinnerService = inject(SpinnerService);
  commentMessage: string = '';

  private vaccinationFormService: VaccinationFormService = inject(VaccinationFormService);
  private matDialog: MatDialog = inject(MatDialog);
  private vaccinationDiseases: VaccinePredefinedDiseases = {};
  private unsubscribe$ = new Subject<void>();

  ngOnInit(): void {
    this.dialogService.showActionSidenav(false);
    this.displayMenu(false, false);

    this.getDropdownData();
    initializeActionData('', this.sharedDataService);
    let id = this.activatedRoute.snapshot.params['id'];
    let role: string = this.sharedDataService.storedData['role']!;
    this.vaccinationService.find(id).subscribe(vaccine => {
      if (vaccine) {
        this.vaccination = vaccine;
        this.updateForm(this.vaccination);
      } else {
        this.vaccinationService.query().subscribe({
          next: list => {
            this.vaccination = list.find(filteredVaccine => filteredVaccine.id === id)!;
            this.updateForm(this.vaccination);
          },
        });
      }
    });

    this.processFormOptions();
    this.confidentialityService.loadConfidentialityOptionsWithDefaultSelection(role, this.confidentialityList, this.editForm);
    this.editForm
      .get('vaccineCode')
      ?.valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe(vaccine => {
        const targetDiseases = this.vaccinationDiseases[vaccine?.code];

        this.diseasesChips.selected = [];
        this.editForm.get('targetDiseases')?.setValue([]);
        targetDiseases?.forEach((disease: unknown) => {
          if (this.diseasesChips) {
            this.diseasesChips.select({ option: { value: disease } } as MatAutocompleteSelectedEvent, this.editForm);
          }
        });
      });
    if (this.vaccination!?.targetDiseases.length > 0) {
      this.diseasesChips.assign(this.editForm);
    }
  }

  ngAfterViewInit(): void {
    setDropDownInitialValue(this.vaccinationFilteredList, this.singleSelect);
    setDropDownInitialValue(this.reasons, this.reason);
    setDropDownInitialValue(this.diseases, this.diseasesList);
    this.editForm.controls['recorder'].get('firstName')?.markAsTouched();
    this.editForm.controls['recorder'].get('lastName')?.markAsTouched();
  }

  save(): void {
    this.editForm.value.comment = buildComment(this.commentMessage);

    const vaccination = { ...this.vaccination, ...this.vaccinationFormService.getVaccination(this.editForm) };
    vaccination.status = this.formOptionsService.getOption('immunizationStatusCode', 'completed');
    vaccination.verificationStatus = this.formOptionsService.getOption(
      'immunizationVerificationStatus',
      this.sessionInfoService.canValidate() ? '59156000' : '76104008'
    );
    vaccination.lotNumber = vaccination.lotNumber || '-';

    this.matDialog
      .open(VaccinationConfirmComponent, {
        width: '60vw',
        data: { value: { ...vaccination }, button: { save: 'buttons.SAVE', saveAndStay: 'buttons.SAVE_AND_STAY' } },
        disableClose: true,
      })
      .afterClosed()
      .subscribe({
        next: (res: { action?: string } = {}) => {
          if (res.action === 'SAVE') {
            if (vaccination.id) {
              this.subscribeToSaveResponse(this.vaccinationService.update(vaccination), true);
            } else {
              this.subscribeToSaveResponse(this.vaccinationService.create(vaccination), true);
            }
          }
          if (res.action === 'SAVE_AND_STAY') {
            this.subscribeToSaveResponse(this.vaccinationService.create(vaccination), false);
            this.formGroupDirective.resetForm();
            this.resetDiseasesSelection();
            this.vaccinationFormService.resetMandatoryFields(this.editForm);
          }
        },
      });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  protected onSaveSuccess(navigate: boolean): void {
    if (navigate) {
      routecall(this.router, this.sessionInfoService, '/vaccination');
    } else {
      openSnackBar(this.translateService, this.snackBar, 'HELP.VACCINATION.SAVE_AND_STAY.BODY');
    }
  }

  protected updateForm(vaccination: IVaccination): void {
    this.vaccination = vaccination;
    this.vaccinationFormService.resetForm(this.editForm, vaccination);
    this.vaccination?.targetDiseases.forEach(() => this.diseasesChips?.assign(this.editForm));
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected subscribeToSaveResponse(result: Observable<IVaccination>, navigate: boolean): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(navigate),
    });
  }

  private processFormOptions(): void {
    this.formOptionsService.getAllOptions().subscribe({
      next: options =>
        options.map(option => {
          this.formOptions.set(
            option.name,
            option.entries!.filter(entry => entry.allowDisplay || entry.code === this.vaccination?.code.code)
          );
          this.vaccinationFilteredList.next(this.formOptions.get('immunizationVaccineCode')!);
          this.reasons.next(this.formOptions.get('immunizationReasonCode')!);
          this.diseases.next(this.formOptions.get('immunizationTargetDesease')!);
        }),
    });
    this.formOptionsService
      .getVaccinationsWithDiseases()
      .pipe(
        map(vaccinations =>
          vaccinations.reduce(
            (acc, vax) => ({
              ...acc,
              [vax.vaccine.code]: vax.targetDiseases,
            }),
            {}
          )
        )
      )
      .subscribe(vaccinationDiseases => {
        this.vaccinationDiseases = vaccinationDiseases;
      });
  }

  private getDropdownData(): void {
    this.diseases.subscribe(res => {
      this.diseasesChips.setupChipsControls(_.cloneDeep(res));
    });
  }

  // Resets the selected items in diseasesChips list
  private resetDiseasesSelection(): void {
    this.diseasesChips.list.forEach(item => {
      if (item.selected) {
        item.selected = false;
      }
    });
  }
}

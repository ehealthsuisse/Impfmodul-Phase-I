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
import { FormControl } from '@angular/forms';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatDialog } from '@angular/material/dialog';
import { MatSelect } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import _ from 'lodash';
import { finalize, map, Observable, ReplaySubject, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SessionInfoService } from '../../../../core/security/session-info.service';
import { IVaccination } from '../../../../model';
import { CommentComponent, CommonCardFooterComponent, FormOptionsService, IValueDTO, MainWrapperComponent } from '../../../../shared';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';
import { ConfidentialityService } from '../../../../shared/component/confidentiality/confidentiality.service';
import { ReusableDateFieldComponent } from '../../../../shared/component/resuable-fields/reusable-date-field/reusable-date-field.component';
import { initializeActionData, openSnackBar, routecall, setDropDownInitialValue } from '../../../../shared/function';
import { VaccinePredefinedDiseases } from '../../../../shared/interfaces/vaccination-with-disease.interface';
import { FilterPipePipe } from '../../../../shared/pipes/filter-pipe.pipe';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { VaccinationConfirmComponent } from '../../helper-components/confirm/vaccination-confirm.component';
import { VaccinationFormGroup, VaccinationFormService } from '../../services/vaccination-form.service';
import { VaccinationService } from '../../services/vaccination.service';
import { ChipsHandler } from './chips-handler';

@Component({
  standalone: true,
  selector: 'vm-vaccination-form',
  templateUrl: './vaccination-form.component.html',
  styleUrls: ['./vaccination-form.component.scss'],
  encapsulation: ViewEncapsulation.None,
  imports: [
    SharedLibsModule,
    MainWrapperComponent,
    CommonCardFooterComponent,
    CommentComponent,
    FilterPipePipe,
    ReusableDateFieldComponent,
  ],
})
export class VaccinationFormComponent extends BreakPointSensorComponent implements OnInit, AfterViewInit, OnDestroy {
  vaccinationFilteredList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  reasons: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  diseases: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  vaccinationFilterControl: FormControl = new FormControl();

  @ViewChild('singleSelect', { static: true }) singleSelect!: MatSelect;
  @ViewChild('status', { static: true }) staus!: MatSelect;
  @ViewChild('reason', { static: true }) reason!: MatSelect;
  @ViewChild('diseasesChipsList', { static: true }) diseasesList!: MatSelect;

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
    this.editForm
      .get('vaccineCode')
      ?.valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe(vaccine => {
        const targetDiseases = this.vaccinationDiseases[vaccine.code];

        this.diseasesChips.selected = [];
        this.editForm.get('targetDiseases')?.setValue([]);
        targetDiseases.forEach((disease: unknown) => {
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
    if (this.editForm.value.commentMessage) {
      const commentObj = {
        text: this.editForm.value.commentMessage,
        author: 'will be added by the system',
      };
      this.editForm.value.comments = Object.assign([], this.editForm.value.comments);
      this.editForm.value.comments!.push(commentObj);
    }
    const vaccination = { ...this.vaccination, ...this.vaccinationFormService.getVaccination(this.editForm) };
    vaccination.status = this.formOptionsService.getOption('immunizationStatusCode', 'completed');
    vaccination.lotNumber = vaccination.lotNumber || '-';

    /* eslint-disable-next-line security/detect-non-literal-fs-filename -- Safe as no value holds user input */
    this.matDialog
      .open(VaccinationConfirmComponent, {
        width: '60vw',
        data: { value: { ...vaccination }, button: { save: 'buttons.SAVE', saveAndStay: 'buttons.SAVE_AND_STAY' } },
        disableClose: true,
      })
      .afterClosed()
      .subscribe({
        next: (res: { action?: string } = {}) => {
          vaccination.confidentiality = this.confidentialityService.confidentialityStatus;
          if (res.action === 'SAVE') {
            if (vaccination.id) {
              this.subscribeToSaveResponse(this.vaccinationService.update(vaccination), true);
            } else {
              this.subscribeToSaveResponse(this.vaccinationService.create(vaccination), true);
            }
          }
          if (res.action === 'SAVE_AND_STAY') {
            this.subscribeToSaveResponse(this.vaccinationService.create(vaccination), false);
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
}

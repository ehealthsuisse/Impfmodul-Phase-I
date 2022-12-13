﻿/**
 * Copyright (c) 2022 eHealth Suisse
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
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatMomentDateModule } from '@angular/material-moment-adapter';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSelect, MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import _ from 'lodash';
import { NgxMatSelectSearchModule } from 'ngx-mat-select-search';
import { finalize, map, Observable, ReplaySubject, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { GenericButtonComponent } from 'src/app/shared/component/generic-button/generic-button.component';
import { Vaccination } from '../../../../model';
import {
  CommentComponent,
  CommonCardFooterComponent,
  FormOptionsService,
  IValueDTO,
  MainWrapperComponent,
  PageTitleTranslateComponent,
} from '../../../../shared';
import { filterDropdownList, setDropDownInitialValue } from '../../../../shared/function';
import { VaccinePredefinedDiseases } from '../../../../shared/interfaces/vaccination-with-disease.interface';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { ConfirmComponent } from '../../helper-components/confirm/confirm.component';
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
    ReactiveFormsModule,
    MatDatepickerModule,
    GenericButtonComponent,
    MatDialogModule,
    MatChipsModule,
    TranslateModule,
    MatCheckboxModule,
    MatSelectModule,
    CommonCardFooterComponent,
    NgxMatSelectSearchModule,
    MatMomentDateModule,
    MainWrapperComponent,
    CommentComponent,
  ],
})
export class VaccinationFormComponent extends PageTitleTranslateComponent implements OnInit, AfterViewInit, OnDestroy {
  vaccinationFilteredList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  vaccinationStatuses: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  reasons: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  diseases: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  vaccinationFilterControl: FormControl = new FormControl();

  @ViewChild('singleSelect', { static: true }) singleSelect!: MatSelect;
  @ViewChild('status', { static: true }) staus!: MatSelect;
  @ViewChild('reason', { static: true }) reason!: MatSelect;
  @ViewChild('diseasesChipsList', { static: true }) diseasesList!: MatSelect;

  isSaving = false;
  vaccinations: Vaccination | null = null;
  editForm: VaccinationFormGroup = inject(VaccinationFormService).createVaccinationFormGroup();
  formOptionsService: FormOptionsService = inject(FormOptionsService);
  vaccination: Vaccination | null = null;
  diseasesChips = new ChipsHandler('disease');
  router = inject(Router);
  formOptions: Map<string, IValueDTO[]> = new Map<string, IValueDTO[]>();
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  vaccinationService: VaccinationService = inject(VaccinationService);
  sharedDataService: SharedDataService = inject(SharedDataService);
  helpDialogTitle = 'HELP.VACCINATION.DETAIL.TITLE';
  helpDialogBody = 'HELP.VACCINATION.DETAIL.BODY';
  isEmbedded!: boolean;
  canValidated!: boolean;

  private vaccinationFormService: VaccinationFormService = inject(VaccinationFormService);
  private matDialog: MatDialog = inject(MatDialog);
  private vaccinationDiseases: VaccinePredefinedDiseases = {};
  private unsubscribe$ = new Subject<void>();

  ngOnInit(): void {
    this.getDropdownData();
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
    this.vaccinationFilterControl.valueChanges.subscribe(() => {
      filterDropdownList(this.formOptions.get('immunizationVaccineCode')!, this.vaccinationFilteredList, this.vaccinationFilterControl);
    });

    this.canValidated = this.sharedDataService.storedData['role'] === 'HCP' || this.sharedDataService.storedData['role'] === 'ASS';
    this.processFormOptions();
    this.editForm
      .get('vaccineCode')
      ?.valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe(vaccine => {
        const targetDiseases = this.vaccinationDiseases[vaccine.code];

        this.diseasesChips.selected = [];
        this.editForm.get('targetDiseases')?.setValue([]);
        targetDiseases.forEach((disease: any) =>
          this.diseasesChips.select({ option: { value: disease } } as MatAutocompleteSelectedEvent, this.editForm)
        );
      });
    if (this.vaccination!?.targetDiseases.length > 0) {
      this.diseasesChips.assign(this.editForm);
    }
  }

  ngAfterViewInit(): void {
    setDropDownInitialValue(this.vaccinationFilteredList, this.singleSelect);
    setDropDownInitialValue(this.vaccinationStatuses, this.staus);
    setDropDownInitialValue(this.reasons, this.reason);
    setDropDownInitialValue(this.diseases, this.diseasesList);
  }

  save(): void {
    if (this.editForm.value.commentMessage) {
      const commentObj = {
        text: this.editForm.value.commentMessage,
        author: {
          firstName: this.sharedDataService.storedData['ufname']!,
          lastName: this.sharedDataService.storedData['ugname']!,
          prefix: this.sharedDataService.storedData['utitle']!,
          role: this.sharedDataService.storedData['role']!,
        },
      };
      this.editForm.value.comments = Object.assign([], this.editForm.value.comments);
      this.editForm.value.comments!.push(commentObj);
    }
    const vaccination = { ...this.vaccination, ...this.vaccinationFormService.getVaccination(this.editForm) };
    if (!vaccination.lotNumber) {
      vaccination.lotNumber = '-';
    }
    /* eslint-disable-next-line security/detect-non-literal-fs-filename -- Safe as no value holds user input */
    this.matDialog
      .open(ConfirmComponent, {
        width: '60vw',
        data: { value: { ...vaccination }, button: 'buttons.SAVE' },
        disableClose: true,
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          vaccination.confidentiality = this.baseServices.confidentialityStatus;
          vaccination.validated = this.canValidated || vaccination.validated;
          if (vaccination.id) {
            this.subscribeToSaveResponse(this.vaccinationService.update(vaccination));
          } else {
            this.subscribeToSaveResponse(this.vaccinationService.create(vaccination));
          }
        }
      });
  }

  override ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
    super.ngOnDestroy();
  }

  protected onSaveSuccess(): void {
    this.router.navigate(['vaccination']);
  }

  protected updateForm(vaccination: Vaccination): void {
    this.vaccination = vaccination;
    this.vaccinationFormService.resetForm(this.editForm, vaccination);
    this.vaccination?.targetDiseases.forEach(() => this.diseasesChips?.assign(this.editForm));
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected subscribeToSaveResponse(result: Observable<Vaccination>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  private processFormOptions(): void {
    this.formOptionsService.getAllOptions().subscribe({
      next: options =>
        options.map(option => {
          this.formOptions.set(option.name, option.entries!);
          this.vaccinationFilteredList.next(this.formOptions.get('immunizationVaccineCode')!);
          this.vaccinationStatuses.next(this.formOptions.get('immunizationStatusCode')!);
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

  private onSaveError(): void {
    alert('error');
  }

  private getDropdownData(): void {
    this.diseases.subscribe(res => {
      this.diseasesChips.setupChipsControls(_.cloneDeep(res));
    });
  }
}

/**
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
import { AfterViewInit, Component, inject, OnInit, ViewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatMomentDateModule } from '@angular/material-moment-adapter';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSelect } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import { NgxMatSelectSearchModule } from 'ngx-mat-select-search';
import { finalize, Observable, ReplaySubject } from 'rxjs';
import { Allergy } from '../../../../model';
import {
  CommentComponent,
  CommonCardFooterComponent,
  FormOptionsService,
  GenericButtonComponent,
  IValueDTO,
  MainWrapperComponent,
  PageTitleTranslateComponent,
} from '../../../../shared';
import { filterDropdownList, setDropDownInitialValue } from '../../../../shared/function';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { AllergyConfirmComponent } from '../../helper-components/confirm/allergy-confirm.component';
import { AllergyFormGroup, AllergyFormService } from '../../services/allergy-form.service';
import { AllergyService } from '../../services/allergy.service';

@Component({
  standalone: true,
  selector: 'vm-allergy-form',
  templateUrl: './allergy-form.component.html',
  styleUrls: ['./allergy-form.component.scss'],
  imports: [
    SharedLibsModule,
    ReactiveFormsModule,
    MatDatepickerModule,
    MatMomentDateModule,
    GenericButtonComponent,
    MatDialogModule,
    CommonCardFooterComponent,
    NgxMatSelectSearchModule,
    MainWrapperComponent,
    CommentComponent,
  ],
})
export class AllergyFormComponent extends PageTitleTranslateComponent implements OnInit, AfterViewInit {
  filteredAllergies: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  allergiesCriticality: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  allergyStatus: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  allergyVerificationStats: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  sharedDataService: SharedDataService = inject(SharedDataService);
  allergiesControl: FormControl = new FormControl();

  @ViewChild('singleSelect', { static: true }) singleSelect!: MatSelect;
  @ViewChild('clinicalStatus', { static: true }) clinicalStatus!: MatSelect;
  @ViewChild('verificationStatus', { static: true }) verificationStatus!: MatSelect;
  @ViewChild('allergyCriticality', { static: true }) allergyCriticality!: MatSelect;
  @ViewChild('allergyType', { static: true }) allergyType!: MatSelect;

  isSaving = false;
  allergies: Allergy | null = null;
  editForm: AllergyFormGroup = inject(AllergyFormService).createAllergyFormGroup();
  formOptionsService: FormOptionsService = inject(FormOptionsService);
  allergy: Allergy | null = null;
  router = inject(Router);
  formOptions: Map<string, IValueDTO[]> = new Map<string, IValueDTO[]>();
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  allergyService: AllergyService = inject(AllergyService);
  helpDialogTitle = 'HELP.ALLERGY.DETAIL.TITLE';
  helpDialogBody = 'HELP.ALLERGY.DETAIL.BODY';
  isEmbedded!: boolean;

  private allergyFormService: AllergyFormService = inject(AllergyFormService);
  private matDialog: MatDialog = inject(MatDialog);

  ngOnInit(): void {
    let id = this.activatedRoute.snapshot.params['id'];
    this.allergyService.find(id).subscribe(allergy => {
      if (allergy) {
        this.allergy = allergy;
        this.updateForm(this.allergy);
      } else {
        this.allergyService.query().subscribe({
          next: list => {
            this.allergy = list.find(filteredAllergy => filteredAllergy.id === id)!;
            this.updateForm(this.allergy);
          },
        });
      }
    });

    this.allergiesControl.valueChanges.subscribe(() => {
      filterDropdownList(this.formOptions.get('allergyCode')!, this.filteredAllergies, this.allergiesControl);
    });

    this.processFormOptions();
  }

  ngAfterViewInit(): void {
    setDropDownInitialValue(this.filteredAllergies, this.singleSelect);
    setDropDownInitialValue(this.allergyStatus, this.clinicalStatus);
    setDropDownInitialValue(this.allergyVerificationStats, this.verificationStatus);
    setDropDownInitialValue(this.allergiesCriticality, this.allergyCriticality);
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
    const allergy = { ...this.allergy, ...this.allergyFormService.getAllergy(this.editForm) };
    /* eslint-disable-next-line security/detect-non-literal-fs-filename -- Safe as no value holds user input */
    this.matDialog
      .open(AllergyConfirmComponent, {
        width: '60vw',
        data: { value: { ...allergy }, button: 'buttons.SAVE' },
        disableClose: true,
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          allergy.confidentiality = this.baseServices.confidentialityStatus;
          if (allergy.id) {
            this.subscribeToSaveResponse(this.allergyService.update(allergy));
          } else {
            this.subscribeToSaveResponse(this.allergyService.create(allergy));
          }
        }
      });
  }

  private processFormOptions(): void {
    this.formOptionsService.getAllOptions().subscribe({
      next: options =>
        options.map(option => {
          this.formOptions.set(option.name, option.entries!);
          this.filteredAllergies.next(this.formOptions.get('allergyCode')!);
          this.allergyStatus.next(this.formOptions.get('allergyClinicalStatus')!);
          this.allergiesCriticality.next(this.formOptions.get('allergyCriticality')!);
          this.allergyVerificationStats.next(this.formOptions.get('allergyVerificationStatus')!);
        }),
    });
  }

  private updateForm(allergy: Allergy): void {
    this.allergy = allergy;
    this.allergyFormService.resetForm(this.editForm, allergy);
  }

  private subscribeToSaveResponse(result: Observable<Allergy>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  private onSaveError(): void {
    alert('error');
  }

  private onSaveSuccess(): void {
    this.router.navigate(['allergy']);
  }

  private onSaveFinalize(): void {
    this.isSaving = false;
  }
}

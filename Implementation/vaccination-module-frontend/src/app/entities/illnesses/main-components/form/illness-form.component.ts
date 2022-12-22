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
import { IIllnesses } from '../../../../model';
import {
  CommentComponent,
  CommonCardFooterComponent,
  FormOptionsService,
  GenericButtonComponent,
  IValueDTO,
  MainWrapperComponent,
} from '../../../../shared';
import { PageTitleTranslateComponent } from '../../../../shared/component/page-title-translate/page-title-translate.component';
import { filterDropdownList, setDropDownInitialValue } from '../../../../shared/function/functions';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { IllnessConfirmComponent } from '../../helper-components/confirm/illness-confirm.component';
import { IllnessesFormGroup, IllnessesFormService } from '../../service/illness-form.service';
import { IllnessService } from '../../service/illness.service';
import { SharedDataService } from '../../../../shared/services/shared-data.service';

@Component({
  standalone: true,
  selector: 'vm-illnesses-update',
  templateUrl: './illness-form.component.html',
  styleUrls: ['./illness-form.component.scss'],
  imports: [
    SharedLibsModule,
    ReactiveFormsModule,
    MatDatepickerModule,
    MatMomentDateModule,
    GenericButtonComponent,
    MatDialogModule,
    CommonCardFooterComponent,
    NgxMatSelectSearchModule,
    CommentComponent,
    MainWrapperComponent,
  ],
})
export class IllnessFormComponent extends PageTitleTranslateComponent implements OnInit, AfterViewInit {
  illnessesFilteredList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  illnessesStatus: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  illnessesVerificationStats: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);

  @ViewChild('singleSelect', { static: true }) singleSelect!: MatSelect;
  @ViewChild('clinicalStatus', { static: true }) clinicalStatus!: MatSelect;
  @ViewChild('verificationStatus', { static: true }) verrificatin!: MatSelect;

  illnessesFilterControl: FormControl = new FormControl();

  isSaving = false;
  illnesses: IIllnesses | null = null;
  editForm: IllnessesFormGroup = inject(IllnessesFormService).createIllnessesFormGroup();
  formOptionsService: FormOptionsService = inject(FormOptionsService);
  illness: IIllnesses | null = null;
  router = inject(Router);
  formOptions: Map<string, IValueDTO[]> = new Map<string, IValueDTO[]>();
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  illnessService: IllnessService = inject(IllnessService);
  helpDialogTitle = 'HELP.ILLNESS.EDIT.TITLE';
  helpDialogBody = 'HELP.PAST_ILLNESSES.EDIT.BODY';
  isEmbedded!: boolean;
  sharedDataService: SharedDataService = inject(SharedDataService);

  private illnessesFormService: IllnessesFormService = inject(IllnessesFormService);
  private matDialog: MatDialog = inject(MatDialog);

  ngOnInit(): void {
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

    this.processFormOptions();
  }

  ngAfterViewInit(): void {
    setDropDownInitialValue(this.illnessesStatus, this.clinicalStatus);
    setDropDownInitialValue(this.illnessesVerificationStats, this.verrificatin);
    setDropDownInitialValue(this.illnessesFilteredList, this.singleSelect);
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
    const illness = { ...this.illness, ...this.illnessesFormService.getIllnesses(this.editForm) };
    /* eslint-disable-next-line security/detect-non-literal-fs-filename -- Safe as no value holds user input */
    this.matDialog
      .open(IllnessConfirmComponent, {
        width: '60vw',
        data: { value: { ...illness }, button: 'buttons.SAVE' },
        disableClose: true,
      })
      .afterClosed()
      .subscribe({
        next: (confirmed: boolean) => {
          if (confirmed) {
            illness.confidentiality = this.baseServices.confidentialityStatus;
            if (illness.id) {
              this.subscribeToSaveResponse(this.illnessService.update(illness));
            } else {
              this.subscribeToSaveResponse(this.illnessService.create(illness));
            }
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
          this.illnessesStatus.next(this.formOptions.get('conditionClinicalStatus')!);
          this.illnessesVerificationStats.next(this.formOptions.get('conditionVerificationStatus')!);
        }),
    });
  }

  private onSaveSuccess(): void {
    this.router.navigate(['/illnesses']);
  }

  private updateForm(illnesses: IIllnesses): void {
    this.illnesses = illnesses;
    this.illnessesFormService.resetForm(this.editForm, illnesses);
  }

  private onSaveFinalize(): void {
    this.isSaving = false;
  }

  private subscribeToSaveResponse(result: Observable<IIllnesses>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  private onSaveError(): void {
    alert('error');
  }
}

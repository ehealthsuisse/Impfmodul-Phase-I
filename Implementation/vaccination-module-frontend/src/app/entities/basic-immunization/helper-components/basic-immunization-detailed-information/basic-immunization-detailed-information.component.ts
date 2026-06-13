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
import { AfterViewInit, ChangeDetectionStrategy, Component, inject, input, OnInit, Optional, ViewChild } from '@angular/core';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { IBasicImmunization } from '../../../../model';
import { ReusableDateFieldComponent } from '../../../../shared/component/resuable-fields/reusable-date-field/reusable-date-field.component';
import { BasicImmunizationFormService, BasicImmunizationFormGroup } from '../../service/basic-immunization-form.service';
import { ConfirmComponent } from '../../../../shared/component/confirm/confirm.component';
import { ReusableSelectFieldComponent } from '../../../../shared/component/resuable-fields/reusable-select-field/reusable-select-field.component';
import { ReusableSelectFieldWithSearchComponent } from '../../../../shared/component/resuable-fields/reusable-select-field-with-search/reusable-select-field-with-search.component';
import { filterDropdownList, setDropDownInitialValue } from '../../../../shared/function';
import { IValueDTO } from '../../../../shared';
import { ReplaySubject } from 'rxjs';
import { MatSelect } from '@angular/material/select';
import { FormControl } from '@angular/forms';

@Component({
  selector: 'vm-basic-immunization-detailed-information',
  imports: [SharedLibsModule, ReusableDateFieldComponent, ReusableSelectFieldComponent, ReusableSelectFieldWithSearchComponent],
  templateUrl: './basic-immunization-detailed-information.component.html',
  styleUrls: ['./basic-immunization-detailed-information.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BasicImmunizationDetailedInformationComponent implements OnInit, AfterViewInit {
  commentsOpened: boolean = false;
  basicImmunization = input<IBasicImmunization | null>(null);
  basicImmunizationForm = input<BasicImmunizationFormGroup>(inject(BasicImmunizationFormService).createBasicImmunizationFormGroup());
  isEditable = input<boolean>(false);
  showTitle = input<boolean>(false);
  @ViewChild('singleSelect', { static: true }) singleSelect!: MatSelect;
  formOptions: Map<string, IValueDTO[]> = new Map<string, IValueDTO[]>();
  basicImmunizationFilteredList: ReplaySubject<any[]> = new ReplaySubject<any[]>(1);
  basicImmunizationFilterControl: FormControl = new FormControl();

  constructor(@Optional() private confirmParent: ConfirmComponent) {}

  get isConfirmComponentParent(): boolean {
    return !!this.confirmParent;
  }

  toggleComments(): void {
    this.commentsOpened = !this.commentsOpened;
  }

  ngOnInit(): void {
    filterDropdownList(
      this.formOptions.get('basicImmunizationCode')!,
      this.basicImmunizationFilteredList,
      this.basicImmunizationFilterControl
    );
  }

  ngAfterViewInit(): void {
    setDropDownInitialValue(this.basicImmunizationFilteredList, this.singleSelect);
  }
}

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
import { ChangeDetectionStrategy, Component, inject, input, Optional } from '@angular/core';
import { ILaboratorySerology } from '../../../../model';
import { IValueDTO } from '../../../../shared';
import { ConfirmComponent } from '../../../../shared/component/confirm/confirm.component';
import { ReusableDateFieldComponent } from '../../../../shared/component/resuable-fields/reusable-date-field/reusable-date-field.component';
import { ReusableRecorderFieldComponent } from '../../../../shared/component/resuable-fields/reusable-recorder-field/reusable-recorder-field.component';
import { ReusableSelectFieldComponent } from '../../../../shared/component/resuable-fields/reusable-select-field/reusable-select-field.component';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { LaboratorySerologyFormGroup, LaboratorySerologyFormService } from '../../services/laboratory-serology-form.service';

@Component({
  selector: 'vm-laboratory-serology-detailed-information',
  standalone: true,
  imports: [SharedLibsModule, ReusableDateFieldComponent, ReusableRecorderFieldComponent, ReusableSelectFieldComponent],
  templateUrl: './laboratory-serology-detailed-information.component.html',
  styleUrls: ['./laboratory-serology-detailed-information.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LaboratorySerologyDetailedInformationComponent {
  laboratorySerology = input<ILaboratorySerology | null>(null);
  showTitle = input(false);
  laboratorySerologyForm = input<LaboratorySerologyFormGroup>(inject(LaboratorySerologyFormService).createFormGroup());

  commentsOpened: boolean = false;

  constructor(@Optional() private confirmParent: ConfirmComponent) {}

  get isConfirmComponentParent(): boolean {
    return !!this.confirmParent;
  }

  toggleComments(): void {
    this.commentsOpened = !this.commentsOpened;
  }

  get formattedValue(): string {
    const value = this.laboratorySerology()?.value;
    if (!value) {
      return '';
    }

    if (typeof value === 'string') {
      return value;
    }

    return this.formatValueDTO(value);
  }

  private formatValueDTO(value: IValueDTO): string {
    return [value.code, value.name].filter(Boolean).join(' ');
  }
}

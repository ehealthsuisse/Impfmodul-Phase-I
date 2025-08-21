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
import { ChangeDetectionStrategy, Component, inject, Input, Optional } from '@angular/core';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { IAdverseEvent } from '../../../../model';
import { AdverseEventFormGroup, AdverseEventFormService } from '../../services/adverse-event-form.service';
import { ConfirmComponent } from '../../../../shared/component/confirm/confirm.component';
import { ReusableDateFieldComponent } from '../../../../shared/component/resuable-fields/reusable-date-field/reusable-date-field.component';
import { ReusableRecorderFieldComponent } from '../../../../shared/component/resuable-fields/reusable-recorder-field/reusable-recorder-field.component';
import { ReusableSelectFieldComponent } from '../../../../shared/component/resuable-fields/reusable-select-field/reusable-select-field.component';

@Component({
  selector: 'vm-adverse-event-detailed-information',
  standalone: true,
  imports: [SharedLibsModule, ReusableDateFieldComponent, ReusableRecorderFieldComponent, ReusableSelectFieldComponent],
  templateUrl: './adverse-event-detailed-information.component.html',
  styleUrls: ['./adverse-event-detailed-information.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdverseEventDetailedInformationComponent {
  @Input() adverseEvent: IAdverseEvent | null = null;
  @Input() showTitle: boolean = false;
  @Input() adverseEventForm: AdverseEventFormGroup = inject(AdverseEventFormService).createAllergyFormGroup();

  commentsOpened: boolean = false;

  constructor(@Optional() private confirmParent: ConfirmComponent) {}

  get isConfirmComponentParent(): boolean {
    return !!this.confirmParent;
  }

  toggleComments(): void {
    this.commentsOpened = !this.commentsOpened;
  }
}

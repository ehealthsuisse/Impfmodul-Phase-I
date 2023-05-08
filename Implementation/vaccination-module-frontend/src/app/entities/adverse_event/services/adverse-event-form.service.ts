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
import { inject, Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import dayjs from 'dayjs';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';
import { IAdverseEvent } from '../../../model';
import { IComment } from '../../../shared';
import { TNewEntity } from '../../../shared/typs/NewEntityType';
import { SessionInfoService } from '../../../core/security/session-info.service';

dayjs.extend(utc);
dayjs.extend(timezone);
/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IAllergy for edit and NewAllergyFormGroupInput for create.
 */
type AdverseEventFormGroupInput = IAdverseEvent | PartialWithRequiredKeyOf<TNewEntity<IAdverseEvent>>;

type AdverseEventFormDefaults = Pick<TNewEntity<IAdverseEvent>, 'id'>;

type AdverseEventFormGroupContent = {
  id: FormControl<IAdverseEvent['id'] | TNewEntity<IAdverseEvent>['id']>;
  code: FormControl<IAdverseEvent['code']>;
  recorder: FormControl<IAdverseEvent['recorder']>;
  occurrenceDate: FormControl<IAdverseEvent['occurrenceDate']>;
  organization: FormControl<IAdverseEvent['organization']>;
  comments: FormControl<IComment[]>;
  commentMessage: FormControl;
  confidentiality: FormControl<IAdverseEvent['confidentiality']>;
};

export type AdverseEventFormGroup = FormGroup<AdverseEventFormGroupContent>;

/**
 * A service for creating and resetting form groups.
 */
@Injectable({ providedIn: 'root' })
export class AdverseEventFormService {
  translateService = inject(TranslateService);
  sessionInfo: SessionInfoService = inject(SessionInfoService);

  getFormDefaults(): AdverseEventFormDefaults {
    return {
      id: null,
    };
  }
  createAllergyFormGroup(allergy: AdverseEventFormGroupInput = { id: null }): AdverseEventFormGroup {
    const allergyRawValue = {
      ...this.getFormDefaults(),
      ...allergy,
    };

    const authorInfo = this.sessionInfo.author.getValue();
    return new FormGroup<AdverseEventFormGroupContent>({
      id: new FormControl(
        { value: allergyRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),

      occurrenceDate: new FormControl(new Date()),
      code: new FormControl(null, Validators.required),
      recorder: new FormGroup({
        firstName: new FormControl(authorInfo.firstName),
        lastName: new FormControl(authorInfo.lastName),
        prefix: new FormControl(authorInfo.prefix),
      }),
      organization: new FormControl(),
      comments: new FormControl([]),
      commentMessage: new FormControl(),
      confidentiality: new FormControl(),
    } as any);
  }

  getAllergy(form: AdverseEventFormGroup): IAdverseEvent {
    const formValue = form.value as IAdverseEvent;
    // adding hours here to avoid displaying the wrong day due to timedifference to UTC time
    return { ...formValue, occurrenceDate: dayjs.utc(formValue.occurrenceDate).tz('Europe/Berlin').startOf('date').add(10, 'hours') };
  }

  resetForm(form: AdverseEventFormGroup, adverseEvent: IAdverseEvent | null): void {
    const allergyRawValue = { ...this.getFormDefaults(), ...adverseEvent };

    form.patchValue({
      ...allergyRawValue,
    } as any);
  }
}

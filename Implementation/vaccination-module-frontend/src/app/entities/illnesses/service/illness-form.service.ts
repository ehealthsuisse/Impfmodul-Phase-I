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
import { inject, Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import dayjs, { Dayjs } from 'dayjs';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';
import { ApplicationConfigService } from '../../../core';
import { IIllnesses } from '../../../model/illnesses.interface';
import { IComment } from '../../../shared';
import { TNewEntity } from '../../../shared/typs/NewEntityType';

dayjs.extend(utc);
dayjs.extend(timezone);
/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 */

type IllnessFormGroupInput = IIllnesses | PartialWithRequiredKeyOf<TNewEntity<IIllnesses>>;

type IllnessFormDefaults = Pick<TNewEntity<IIllnesses>, 'id'>;

type IllnessFormGroupContent = {
  id: FormControl<IIllnesses['id'] | TNewEntity<IIllnesses>['id']>;
  illnessCode: FormControl<IIllnesses['illnessCode']>;
  verificationStatus: FormControl<IIllnesses['verificationStatus']>;
  clinicalStatus: FormControl<IIllnesses['clinicalStatus']>;
  recorder: FormControl<IIllnesses['recorder']>;
  organization: FormControl<IIllnesses['organization']>;
  recordedDate: FormControl<IIllnesses['recordedDate']>;
  begin: FormControl<IIllnesses['begin']>;
  end: FormControl<IIllnesses['end']>;
  comments: FormControl<IComment[]>;
  commentMessage: FormControl;
  confidentiality: FormControl<IIllnesses['confidentiality']>;
};

export type IllnessesFormGroup = FormGroup<IllnessFormGroupContent>;

/**
 * A service for creating and resetting form groups.
 */
@Injectable({ providedIn: 'root' })
export class IllnessesFormService {
  appConfig = inject(ApplicationConfigService);

  getFormDefaults(): IllnessFormDefaults {
    return {
      id: null,
    };
  }
  createIllnessesFormGroup(illness: IllnessFormGroupInput = { id: null }): IllnessesFormGroup {
    const illnessRawValue = {
      ...this.getFormDefaults(),
      ...illness,
    };
    return new FormGroup<IllnessFormGroupContent>({
      id: new FormControl(
        { value: illnessRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),

      recordedDate: new FormControl(new Date(), Validators.required),
      begin: new FormControl(new Date(), Validators.required),
      end: new FormControl(illnessRawValue.end),
      illnessCode: new FormControl(null, Validators.required),
      verificationStatus: new FormControl(),
      clinicalStatus: new FormControl(null, Validators.required),
      recorder: new FormGroup({
        firstName: new FormControl(),
        lastName: new FormControl(),
        prefix: new FormControl(),
      }),
      organization: new FormControl(),
      comments: new FormControl([]),
      commentMessage: new FormControl(),
      confidentiality: new FormControl(),
    } as any);
  }

  getIllnesses(form: IllnessesFormGroup): IIllnesses {
    // adding hours here to avoid displaying the wrong day due to timedifference to UTC time
    const dateWithTimezone = (date: string | Dayjs): Dayjs => dayjs.utc(date).tz('Europe/Berlin').startOf('date').add(10, 'hours');
    const formValue = form.value as IIllnesses;

    return {
      ...formValue,
      recordedDate: dateWithTimezone(formValue.recordedDate),
      begin: dateWithTimezone(formValue.begin),
      end: dateWithTimezone(formValue.end),
    };
  }

  resetForm(form: IllnessesFormGroup, illness: IIllnesses): void {
    const illnessRawValue = { ...this.getFormDefaults(), ...illness };

    form.patchValue({
      ...illnessRawValue,
    } as any);
  }
}

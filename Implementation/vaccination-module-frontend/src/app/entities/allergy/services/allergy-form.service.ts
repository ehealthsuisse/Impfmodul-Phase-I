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
import { TranslateService } from '@ngx-translate/core';
import dayjs from 'dayjs';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';
import { ApplicationConfigService } from '../../../core';
import { Allergy } from '../../../model';
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
 * It accepts IAllergy for edit and NewAllergyFormGroupInput for create.
 */
type AllergyFormGroupInput = Allergy | PartialWithRequiredKeyOf<TNewEntity<Allergy>>;

type AllergyFormDefaults = Pick<TNewEntity<Allergy>, 'id'>;

type AllergyFormGroupContent = {
  id: FormControl<Allergy['id'] | TNewEntity<Allergy>['id']>;
  allergyCode: FormControl<Allergy['code']>;
  criticality: FormControl<Allergy['criticality']>;
  verificationStatus: FormControl<Allergy['verificationStatus']>;
  clinicalStatus: FormControl<Allergy['clinicalStatus']>;
  recorder: FormControl<Allergy['recorder']>;
  occurrenceDate: FormControl<Allergy['occurrenceDate']>;
  organization: FormControl<Allergy['organization']>;
  comments: FormControl<IComment[]>;
  commentMessage: FormControl;
  confidentiality: FormControl<Allergy['confidentiality']>;
};

export type AllergyFormGroup = FormGroup<AllergyFormGroupContent>;

/**
 * A service for creating and resetting form groups.
 */
@Injectable({ providedIn: 'root' })
export class AllergyFormService {
  appConfig = inject(ApplicationConfigService);
  translateService = inject(TranslateService);

  getFormDefaults(): AllergyFormDefaults {
    return {
      id: null,
    };
  }
  createAllergyFormGroup(allergy: AllergyFormGroupInput = { id: null }): AllergyFormGroup {
    const allergyRawValue = {
      ...this.getFormDefaults(),
      ...allergy,
    };

    return new FormGroup<AllergyFormGroupContent>({
      id: new FormControl(
        { value: allergyRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),

      occurrenceDate: new FormControl(new Date()),
      allergyCode: new FormControl(null, Validators.required),
      verificationStatus: new FormControl(null, Validators.required),
      clinicalStatus: new FormControl(null, Validators.required),
      criticality: new FormControl(),
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

  getAllergy(form: AllergyFormGroup): Allergy {
    const formValue = form.value as Allergy;

    // adding hours here to avoid displaying the wrong day due to timedifference to UTC time
    return { ...formValue, occurrenceDate: dayjs.utc(formValue.occurrenceDate).tz('Europe/Berlin').startOf('date').add(10, 'hours') };
  }

  resetForm(form: AllergyFormGroup, allergy: Allergy | null): void {
    const allergyRawValue = { ...this.getFormDefaults(), ...allergy };

    form.patchValue({
      ...allergyRawValue,
    } as any);
  }
}

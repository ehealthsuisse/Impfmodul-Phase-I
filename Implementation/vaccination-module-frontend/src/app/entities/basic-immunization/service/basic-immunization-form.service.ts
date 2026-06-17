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
import { inject, Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import dayjs, { Dayjs } from 'dayjs';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';
import { IBasicImmunization } from 'src/app/model/basic-immunization.interface';
import { SessionInfoService } from '../../../core/security/session-info.service';
import { notFutureDateValidator } from '../../../core/validators/date-order-validator';
import { IComment } from '../../../shared';
import { PartialWithRequiredKeyOf, TNewEntity } from '../../../shared/typs/NewEntityType';
import { extractSessionDetailsByRole, normalizeOrganization, normalizeRecorder, setDefaultValues } from '../../../shared/function';

dayjs.extend(utc);
dayjs.extend(timezone);

/**
 * Type for createFormGroup and resetForm argument.
 */
type BasicImmunizationFormGroupInput = IBasicImmunization | PartialWithRequiredKeyOf<TNewEntity<IBasicImmunization>>;

type BasicImmunizationFormDefaults = Pick<TNewEntity<IBasicImmunization>, 'id'>;

type BasicImmunizationFormGroupContent = {
  id: FormControl<IBasicImmunization['id'] | TNewEntity<IBasicImmunization>['id']>;
  code: FormControl<IBasicImmunization['code']>;
  category: FormControl<IBasicImmunization['category']>;
  verificationStatus: FormControl<IBasicImmunization['verificationStatus']>;
  clinicalStatus: FormControl<IBasicImmunization['clinicalStatus']>;
  recorder: FormControl<IBasicImmunization['recorder']>;
  organization: FormControl<IBasicImmunization['organization']>;
  onsetDate: FormControl<IBasicImmunization['onsetDate']>;
  recordedDate: FormControl<IBasicImmunization['recordedDate']>;
  targetDiseases: FormControl<IBasicImmunization['targetDiseases']>;
  relatesTo: FormControl<IBasicImmunization['relatesTo']>;
  comment: FormControl<IComment>;
  commentMessage: FormControl;
  confidentiality: FormControl<IBasicImmunization['confidentiality']>;
};

export type BasicImmunizationFormGroup = FormGroup<BasicImmunizationFormGroupContent>;

/**
 * A service for creating and resetting form groups.
 */
@Injectable({ providedIn: 'root' })
export class BasicImmunizationFormService {
  sessionInfo: SessionInfoService = inject(SessionInfoService);

  getFormDefaults(): BasicImmunizationFormDefaults {
    return {
      id: null,
    };
  }

  createBasicImmunizationFormGroup(basicImmunization: BasicImmunizationFormGroupInput = { id: null }): BasicImmunizationFormGroup {
    const rawValue = {
      ...this.getFormDefaults(),
      ...basicImmunization,
    };

    const extractSessionDetails = extractSessionDetailsByRole(this.sessionInfo);
    return new FormGroup<BasicImmunizationFormGroupContent>({
      id: new FormControl(
        { value: rawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),
      onsetDate: new FormControl(new Date(), [Validators.required, notFutureDateValidator('onsetDate')]),
      recordedDate: new FormControl(new Date(), [Validators.required, notFutureDateValidator('recordedDate')]),
      code: new FormControl(null, Validators.required),
      category: new FormControl(null),
      verificationStatus: new FormControl(),
      clinicalStatus: new FormControl(),
      targetDiseases: new FormControl(null),
      relatesTo: new FormControl(null),
      recorder: new FormGroup({
        firstName: new FormControl(extractSessionDetails.firstName),
        lastName: new FormControl(extractSessionDetails.lastName),
        prefix: new FormControl(extractSessionDetails.prefix),
      }),
      organization: new FormControl(extractSessionDetails.organization),
      comment: new FormControl(),
      commentMessage: new FormControl(),
      confidentiality: new FormControl(null, Validators.required),
    } as any);
  }

  getBasicImmunization(form: BasicImmunizationFormGroup): IBasicImmunization {
    const dateWithTimezone = (date: string | Dayjs): Dayjs => dayjs.utc(date).tz('Europe/Berlin').startOf('date').add(10, 'hours');
    const formValue = form.value as IBasicImmunization;
    formValue.recorder = normalizeRecorder(formValue.recorder);
    formValue.organization = normalizeOrganization(formValue.organization);

    return {
      ...formValue,
      onsetDate: dateWithTimezone(formValue.onsetDate),
      recordedDate: dateWithTimezone(formValue.recordedDate),
    };
  }

  resetForm(form: BasicImmunizationFormGroup, basicImmunization: IBasicImmunization): void {
    const rawValue = { ...this.getFormDefaults(), ...basicImmunization };

    form.patchValue({
      ...rawValue,
    } as any);

    if (basicImmunization?.recorder === null) {
      form.get('recorder')?.reset();
    }
  }

  resetMandatoryFields(form: BasicImmunizationFormGroup): void {
    const fieldsToReset = [
      { name: 'code', value: null },
      { name: 'onsetDate', value: new Date() },
      { name: 'recordedDate', value: new Date() },
      { name: 'confidentiality', value: { code: '17621005', name: 'Normal', system: '2.16.840.1.113883.6.96' } },
    ];

    fieldsToReset.forEach(field => setDefaultValues(form, field.name, field.value));
  }
}

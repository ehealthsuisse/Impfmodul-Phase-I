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
import dayjs, { Dayjs } from 'dayjs';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';
import { SessionInfoService } from '../../../core/security/session-info.service';
import { dateValidator, notFutureDateValidator, validateDatesNotBefore } from '../../../core/validators/date-order-validator';
import { IInfectiousDiseases } from '../../../model';
import { IComment, IHumanDTO } from '../../../shared';
import { TNewEntity } from '../../../shared/typs/NewEntityType';
import { extractSessionDetailsByRole, setDefaultValues } from '../../../shared/function';

dayjs.extend(utc);
dayjs.extend(timezone);
/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 */

type InfectiousDiseasesFormGroupInput = IInfectiousDiseases | PartialWithRequiredKeyOf<TNewEntity<IInfectiousDiseases>>;

type InfectiousDiseasesFormDefaults = Pick<TNewEntity<IInfectiousDiseases>, 'id'>;

type InfectiousDiseasesFormGroupContent = {
  id: FormControl<IInfectiousDiseases['id'] | TNewEntity<IInfectiousDiseases>['id']>;
  code: FormControl<IInfectiousDiseases['code']>;
  recorder: FormControl<IInfectiousDiseases['recorder']>;
  organization: FormControl<IInfectiousDiseases['organization']>;
  recordedDate: FormControl<IInfectiousDiseases['recordedDate']>;
  begin: FormControl<IInfectiousDiseases['begin']>;
  end: FormControl<IInfectiousDiseases['end']>;
  comment: FormControl<IComment>;
  commentMessage: FormControl;
  confidentiality: FormControl<IInfectiousDiseases['confidentiality']>;
};

export type InfectiousDiseasesFormGroup = FormGroup<InfectiousDiseasesFormGroupContent>;

/**
 * A service for creating and resetting form groups.
 */
@Injectable({ providedIn: 'root' })
export class InfectiousDiseasesFormService {
  sessionInfo: SessionInfoService = inject(SessionInfoService);

  getFormDefaults(): InfectiousDiseasesFormDefaults {
    return {
      id: null,
    };
  }
  createInfectiousDiseasesFormGroup(infectiousDiseases: InfectiousDiseasesFormGroupInput = { id: null }): InfectiousDiseasesFormGroup {
    const illnessRawValue = {
      ...this.getFormDefaults(),
      ...infectiousDiseases,
    };

    const extractSessionDetails = extractSessionDetailsByRole(this.sessionInfo);
    return new FormGroup<InfectiousDiseasesFormGroupContent>({
      id: new FormControl(
        { value: illnessRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),

      recordedDate: new FormControl(new Date(), [
        Validators.required,
        notFutureDateValidator('recordedDate'),
        validateDatesNotBefore('recordedDate', 'begin'),
      ]),
      begin: new FormControl(new Date(), [Validators.required, notFutureDateValidator('begin')]),
      end: new FormControl(null, [dateValidator('begin', 'end')]),
      code: new FormControl(null, Validators.required),
      recorder: new FormGroup({
        firstName: new FormControl(extractSessionDetails.firstName),
        lastName: new FormControl(extractSessionDetails.lastName),
        prefix: new FormControl(extractSessionDetails.prefix),
      }),
      organization: new FormControl(extractSessionDetails.organization),
      comment: new FormControl(),
      commentMessage: new FormControl(),
      confidentiality: new FormControl(),
    } as any);
  }

  getInfectiousDiseases(form: InfectiousDiseasesFormGroup): IInfectiousDiseases {
    // adding hours here to avoid displaying the wrong day due to timedifference to UTC time
    const dateWithTimezone = (date: string | Dayjs): Dayjs => dayjs.utc(date).tz('Europe/Berlin').startOf('date').add(10, 'hours');
    const formValue = form.value as IInfectiousDiseases;
    if (!formValue.recorder?.firstName && !formValue.recorder?.lastName) {
      formValue.recorder = undefined;
    }

    return {
      ...formValue,
      recordedDate: dateWithTimezone(formValue.recordedDate),
      begin: dateWithTimezone(formValue.begin),
      end: formValue.end !== null ? dateWithTimezone(formValue.end) : null,
    };
  }

  resetForm(form: InfectiousDiseasesFormGroup, infectiousDisease: IInfectiousDiseases): void {
    const infectiousDiseasesRawValue = { ...this.getFormDefaults(), ...infectiousDisease };

    form.patchValue({
      ...infectiousDiseasesRawValue,
    } as any);

    if (infectiousDisease?.recorder === null) {
      form.get('recorder')?.reset();
    }
  }

  resetMandatoryFields(form: InfectiousDiseasesFormGroup): void {
    const extractSessionDetails = extractSessionDetailsByRole(this.sessionInfo);
    const recorder: IHumanDTO = {
      firstName: extractSessionDetails.firstName,
      lastName: extractSessionDetails.lastName,
      prefix: extractSessionDetails.prefix,
    };

    const fieldsToReset = [
      { name: 'code', value: null },
      { name: 'recordedDate', value: new Date() },
      { name: 'begin', value: new Date() },
      { name: 'organization', value: extractSessionDetails.organization },
      { name: 'recorder', value: recorder },
      { name: 'confidentiality', value: { code: '17621005', name: 'Normal' } },
    ];

    fieldsToReset.forEach(field => setDefaultValues(form, field.name, field.value));
  }
}

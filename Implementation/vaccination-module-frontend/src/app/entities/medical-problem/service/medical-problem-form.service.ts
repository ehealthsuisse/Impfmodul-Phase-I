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
import { IMedicalProblem } from 'src/app/model/medical-problem.interface';
import { SessionInfoService } from '../../../core/security/session-info.service';
import { dateValidator, notFutureDateValidator } from '../../../core/validators/date-order-validator';
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

type ProblemFormGroupInput = IMedicalProblem | PartialWithRequiredKeyOf<TNewEntity<IMedicalProblem>>;

type ProblemFormDefaults = Pick<TNewEntity<IMedicalProblem>, 'id'>;

type ProblemFormGroupContent = {
  id: FormControl<IMedicalProblem['id'] | TNewEntity<IMedicalProblem>['id']>;
  code: FormControl<IMedicalProblem['code']>;
  verificationStatus: FormControl<IMedicalProblem['verificationStatus']>;
  clinicalStatus: FormControl<IMedicalProblem['clinicalStatus']>;
  recorder: FormControl<IMedicalProblem['recorder']>;
  organization: FormControl<IMedicalProblem['organization']>;
  recordedDate: FormControl<IMedicalProblem['recordedDate']>;
  begin: FormControl<IMedicalProblem['begin']>;
  end: FormControl<IMedicalProblem['end']>;
  comment: FormControl<IComment>;
  commentMessage: FormControl;
  confidentiality: FormControl<IMedicalProblem['confidentiality']>;
};

export type ProblemFormGroup = FormGroup<ProblemFormGroupContent>;

/**
 * A service for creating and resetting form groups.
 */
@Injectable({ providedIn: 'root' })
export class MedicalProblemFormService {
  sessionInfo: SessionInfoService = inject(SessionInfoService);
  getFormDefaults(): ProblemFormDefaults {
    return {
      id: null,
    };
  }
  createProblemFormGroup(problem: ProblemFormGroupInput = { id: null }): ProblemFormGroup {
    const medicalProblemRawValue = {
      ...this.getFormDefaults(),
      ...problem,
    };

    const extractSessionDetails = extractSessionDetailsByRole(this.sessionInfo);
    return new FormGroup<ProblemFormGroupContent>({
      id: new FormControl(
        { value: medicalProblemRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),

      recordedDate: new FormControl(new Date(), [Validators.required, notFutureDateValidator('recordedDate')]),
      begin: new FormControl(new Date(), [Validators.required]),
      end: new FormControl(null, [dateValidator('begin', 'end')]),
      code: new FormControl(null, Validators.required),
      verificationStatus: new FormControl(),
      clinicalStatus: new FormControl(null, Validators.required),
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

  getProblem(form: ProblemFormGroup): IMedicalProblem {
    // adding hours here to avoid displaying the wrong day due to timedifference to UTC time
    const dateWithTimezone = (date: string | Dayjs): Dayjs => dayjs.utc(date).tz('Europe/Berlin').startOf('date').add(10, 'hours');
    const formValue = form.value as IMedicalProblem;
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

  resetForm(form: ProblemFormGroup, medicalProblem: IMedicalProblem): void {
    const medicalProblemRawValue = { ...this.getFormDefaults(), ...medicalProblem };

    form.patchValue({
      ...medicalProblemRawValue,
    } as any);

    if (medicalProblem?.recorder === null) {
      form.get('recorder')?.reset();
    }
  }

  resetMandatoryFields(form: ProblemFormGroup): void {
    const extractSessionDetails = extractSessionDetailsByRole(this.sessionInfo);
    const recorder: IHumanDTO = {
      firstName: extractSessionDetails.firstName,
      lastName: extractSessionDetails.lastName,
      prefix: extractSessionDetails.prefix,
    };

    const fieldsToReset = [
      { name: 'code', value: null },
      { name: 'clinicalStatus', value: { code: '', name: '' } },
      { name: 'recordedDate', value: new Date() },
      { name: 'begin', value: new Date() },
      { name: 'organization', value: extractSessionDetails.organization },
      { name: 'recorder', value: recorder },
      { name: 'confidentiality', value: { code: '17621005', name: 'Normal' } },
    ];

    fieldsToReset.forEach(field => setDefaultValues(form, field.name, field.value));
  }
}

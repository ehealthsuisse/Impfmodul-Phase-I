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
import dayjs from 'dayjs';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';
import { SessionInfoService } from '../../../core/security/session-info.service';
import { IMedicalProblem, IVaccination } from '../../../model';
import { IComment, IHumanDTO, IValueDTO } from '../../../shared';
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
 * It accepts IVaccination for edit and NewVaccinationFormGroupInput for create.
 */
type VaccinationFormGroupInput = IVaccination | PartialWithRequiredKeyOf<TNewEntity<IVaccination>>;

type VaccinationFormDefaults = Pick<TNewEntity<IVaccination>, 'id'>;

type VaccinationFormGroupContent = {
  vaccineCode: FormControl<IVaccination['vaccineCode']>;
  targetDiseases: FormControl<IVaccination['targetDiseases']>;
  doseNumber: FormControl<IVaccination['doseNumber']>;
  occurrenceDate: FormControl<IVaccination['occurrenceDate']>;
  lotNumber: FormControl<IVaccination['lotNumber']>;
  validated: FormControl<IVaccination['validated']>;
  reason: FormControl<IVaccination['reason']>;
  recorder: FormControl<IVaccination['recorder']>;
  author: FormControl<IVaccination['recorder']>;
  organization: FormControl<IVaccination['organization']>;
  comment: FormControl<IComment>;
  commentMessage: FormControl;
  confidentiality: FormControl<IMedicalProblem['confidentiality']>;
};

export type VaccinationFormGroup = FormGroup<VaccinationFormGroupContent>;

/**
 * A service for creating and resetting form groups.
 */
@Injectable({ providedIn: 'root' })
export class VaccinationFormService {
  sessionInfo: SessionInfoService = inject(SessionInfoService);
  validated: boolean = false;

  getFormDefaults(): VaccinationFormDefaults {
    return {
      id: null,
    };
  }

  createVaccinationFormGroup(vaccination: VaccinationFormGroupInput = { id: null }): VaccinationFormGroup {
    const vaccinationRawValue = {
      ...this.getFormDefaults(),
      ...vaccination,
    };

    const extractSessionDetails = extractSessionDetailsByRole(this.sessionInfo);
    return new FormGroup<VaccinationFormGroupContent>({
      id: new FormControl(
        { value: vaccinationRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),

      targetDiseases: new FormControl(null, Validators.required),
      doseNumber: new FormControl(null, [Validators.required, Validators.min(1), Validators.pattern('^(0|[1-9][0-9]*)$')]),
      occurrenceDate: new FormControl(new Date(), Validators.required),
      lotNumber: new FormControl(),
      reason: new FormControl(),
      validated: new FormControl(this.validated),
      vaccineCode: new FormControl(null, Validators.required),
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

  getVaccination(form: VaccinationFormGroup): IVaccination | TNewEntity<IVaccination> {
    const vaccination: IVaccination = form.value as IVaccination;
    vaccination.status = vaccination.status as unknown as IValueDTO;
    if (!vaccination.recorder?.firstName && !vaccination.recorder?.lastName) {
      vaccination.recorder = undefined;
    }
    // adding hours here to avoid displaying the wrong day due to timedifference to UTC time
    vaccination.occurrenceDate = dayjs.utc(vaccination.occurrenceDate).tz('Europe/Berlin').startOf('date').add(10, 'hours');
    return { ...vaccination };
  }

  resetForm(form: VaccinationFormGroup, vaccination: IVaccination | null): void {
    const vaccinationRawValue = { ...this.getFormDefaults(), ...vaccination };

    form.patchValue(
      {
        ...vaccinationRawValue,
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */
    );

    if (vaccination?.recorder === null) {
      form.get('recorder')?.reset();
    }
  }

  resetMandatoryFields(form: VaccinationFormGroup): void {
    const extractSessionDetails = extractSessionDetailsByRole(this.sessionInfo);
    const recorder: IHumanDTO = {
      firstName: extractSessionDetails.firstName,
      lastName: extractSessionDetails.lastName,
      prefix: extractSessionDetails.prefix,
    };

    const fieldsToReset = [
      { name: 'occurrenceDate', value: new Date() },
      { name: 'vaccineCode', value: null },
      { name: 'targetDiseases', value: [] },
      { name: 'doseNumber', value: null },
      { name: 'organization', value: extractSessionDetails.organization },
      { name: 'recorder', value: recorder },
      { name: 'validated', value: extractSessionDetails.validated },
      { name: 'confidentiality', value: { code: '17621005', name: 'Normal' } },
    ];

    fieldsToReset.forEach(field => setDefaultValues(form, field.name, field.value));
  }
}

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
import { IVaccination } from '../../../model';
import { IComment, IValueDTO } from '../../../shared';
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
  comments: FormControl<IComment[]>;
  commentMessage: FormControl;
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

    const isHCP = this.sessionInfo.queryParams.role === 'HCP';
    this.validated = isHCP || this.sessionInfo.queryParams.role === 'ASS';

    const authorInfo = this.sessionInfo.author.getValue();

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
        firstName: new FormControl(authorInfo.firstName),
        lastName: new FormControl(authorInfo.lastName),
        prefix: new FormControl(authorInfo.prefix),
      }),
      organization: new FormControl(),
      comments: new FormControl([]),
      commentMessage: new FormControl(),
    } as any);
  }

  getVaccination(form: VaccinationFormGroup): IVaccination | TNewEntity<IVaccination> {
    const vaccination: IVaccination = form.value as IVaccination;
    vaccination.status = vaccination.status as unknown as IValueDTO;
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
  }
}

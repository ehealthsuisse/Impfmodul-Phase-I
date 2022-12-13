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
import dayjs from 'dayjs';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';
import { ApplicationConfigService } from '../../../core';
import { Vaccination } from '../../../model';
import { IComment, IValueDTO } from '../../../shared';
import { SharedDataService } from '../../../shared/services/shared-data.service';
import { TNewEntity } from '../../../shared/typs/NewEntityType';

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
type VaccinationFormGroupInput = Vaccination | PartialWithRequiredKeyOf<TNewEntity<Vaccination>>;

type VaccinationFormDefaults = Pick<TNewEntity<Vaccination>, 'id'>;

type VaccinationFormGroupContent = {
  vaccineCode: FormControl<Vaccination['vaccineCode']>;
  targetDiseases: FormControl<Vaccination['targetDiseases']>;
  doseNumber: FormControl<Vaccination['doseNumber']>;
  occurrenceDate: FormControl<Vaccination['occurrenceDate']>;
  lotNumber: FormControl<Vaccination['lotNumber']>;
  validated: FormControl<Vaccination['validated']>;
  reason: FormControl<Vaccination['reason']>;
  status: FormControl<Vaccination['status']>;
  recorder: FormControl<Vaccination['recorder']>;
  author: FormControl<Vaccination['recorder']>;
  organization: FormControl<Vaccination['organization']>;
  comments: FormControl<IComment[]>;
  commentMessage: FormControl;
};

export type VaccinationFormGroup = FormGroup<VaccinationFormGroupContent>;
/**
 * A service for creating and resetting form groups.
 */
@Injectable({ providedIn: 'root' })
export class VaccinationFormService {
  appConfig: ApplicationConfigService = inject(ApplicationConfigService);
  sharedDataService: SharedDataService = inject(SharedDataService);

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
    this.validated = this.sharedDataService.storedData['role'] === 'HCP' || this.sharedDataService.storedData['role'] === 'ASS';
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
      status: new FormControl(null, Validators.required),
      vaccineCode: new FormControl(null, Validators.required),
      recorder: new FormGroup({
        firstName: new FormControl(),
        lastName: new FormControl(),
        prefix: new FormControl(),
      }),
      organization: new FormControl(),
      comments: new FormControl([]),
      commentMessage: new FormControl(),
    } as any);
  }

  getVaccination(form: VaccinationFormGroup): Vaccination | TNewEntity<Vaccination> {
    const vaccination: Vaccination = form.value as Vaccination;
    vaccination.status = vaccination.status as unknown as IValueDTO;
    // adding hours here to avoid displaying the wrong day due to timedifference to UTC time
    vaccination.occurrenceDate = dayjs.utc(vaccination.occurrenceDate).tz('Europe/Berlin').startOf('date').add(10, 'hours');
    return { ...vaccination };
  }

  resetForm(form: VaccinationFormGroup, vaccination: Vaccination | null): void {
    const vaccinationRawValue = { ...this.getFormDefaults(), ...vaccination };

    form.patchValue(
      {
        ...vaccinationRawValue,
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */
    );
  }
}

/**
 * Copyright (c) 2026 eHealth Suisse
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
import { notFutureDateValidator } from '../../../core/validators/date-order-validator';
import { ILaboratorySerology } from '../../../model';
import { IComment, IHumanDTO, IValueDTO } from '../../../shared';
import { PartialWithRequiredKeyOf, TNewEntity } from '../../../shared/typs/NewEntityType';
import { extractSessionDetailsByRole, normalizeOrganization, normalizeRecorder, setDefaultValues } from '../../../shared/function';

const dayjsLib = dayjs;

dayjsLib.extend(utc);
dayjsLib.extend(timezone);

// Type for createFormGroup and resetForm argument.
type LaboratorySerologyFormGroupInput = ILaboratorySerology | PartialWithRequiredKeyOf<TNewEntity<ILaboratorySerology>>;

type LaboratorySerologyFormDefaults = Pick<TNewEntity<ILaboratorySerology>, 'id'>;

type LaboratorySerologyFormGroupContent = {
  id: FormControl<ILaboratorySerology['id'] | TNewEntity<ILaboratorySerology>['id']>;
  code: FormControl<ILaboratorySerology['code']>;
  recordedDate: FormControl<ILaboratorySerology['recordedDate']>;
  status: FormControl<ILaboratorySerology['status']>;
  valueCode: FormControl<ILaboratorySerology['value']>;
  valueUnit: FormControl<IValueDTO | null>;
  recorder: FormControl<ILaboratorySerology['recorder']>;
  organization: FormControl<ILaboratorySerology['organization']>;
  comment: FormControl<IComment>;
  commentMessage: FormControl;
  confidentiality: FormControl<ILaboratorySerology['confidentiality']>;
};

export type LaboratorySerologyFormGroup = FormGroup<LaboratorySerologyFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class LaboratorySerologyFormService {
  sessionInfo: SessionInfoService = inject(SessionInfoService);

  getFormDefaults(): LaboratorySerologyFormDefaults {
    return {
      id: null,
    };
  }

  createFormGroup(laboratorySerology: LaboratorySerologyFormGroupInput = { id: null }): LaboratorySerologyFormGroup {
    const rawValue = {
      ...this.getFormDefaults(),
      ...laboratorySerology,
    };

    const extractSessionDetails = extractSessionDetailsByRole(this.sessionInfo);
    return new FormGroup<LaboratorySerologyFormGroupContent>({
      id: new FormControl(
        { value: rawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),
      recordedDate: new FormControl(new Date(), [Validators.required, notFutureDateValidator('recordedDate')]),
      code: new FormControl(null, Validators.required),
      status: new FormControl(null, Validators.required),
      valueCode: new FormControl(null, Validators.required),
      valueUnit: new FormControl(null, Validators.required),
      recorder: new FormGroup({
        firstName: new FormControl(extractSessionDetails.firstName),
        lastName: new FormControl(extractSessionDetails.lastName),
        prefix: new FormControl(extractSessionDetails.prefix),
      } as any),
      organization: new FormControl(extractSessionDetails.organization),
      comment: new FormControl(),
      commentMessage: new FormControl(),
      confidentiality: new FormControl(null, Validators.required),
    } as any);
  }

  getLaboratorySerology(form: LaboratorySerologyFormGroup): ILaboratorySerology {
    const formValue = form.value as ILaboratorySerology & { valueCode?: string | null; valueUnit?: IValueDTO | null };
    formValue.recorder = normalizeRecorder(formValue.recorder);
    formValue.organization = normalizeOrganization(formValue.organization);

    const valueCode = form.get('valueCode')?.value;
    const valueUnit = form.get('valueUnit')?.value;
    const valueDto =
      valueCode !== null && valueCode !== undefined && String(valueCode).trim() !== '' && valueUnit
        ? ({
            code: String(valueCode).trim(),
            name: valueUnit.name,
            system: valueUnit.system,
          } as IValueDTO)
        : null;

    return {
      ...formValue,
      value: valueDto,
      recordedDate: dayjsLib.utc(formValue.recordedDate).tz('Europe/Berlin').startOf('date').add(10, 'hours'),
    } as ILaboratorySerology;
  }

  resetForm(form: LaboratorySerologyFormGroup, laboratorySerology: ILaboratorySerology | null): void {
    const value = typeof laboratorySerology?.value === 'string' ? null : laboratorySerology?.value;
    const rawValue = { ...this.getFormDefaults(), ...laboratorySerology };
    form.patchValue({
      ...rawValue,
      valueCode: value?.code ?? null,
      valueUnit: value ?? null,
    } as any);

    if (laboratorySerology?.recorder === null) {
      form.get('recorder')?.reset();
    }
  }

  resetMandatoryFields(form: LaboratorySerologyFormGroup): void {
    const extractSessionDetails = extractSessionDetailsByRole(this.sessionInfo);
    const recorder: IHumanDTO = {
      firstName: extractSessionDetails.firstName,
      lastName: extractSessionDetails.lastName,
      prefix: extractSessionDetails.prefix,
    };
    const fieldsToReset = [
      { name: 'code', value: null },
      { name: 'recordedDate', value: new Date() },
      { name: 'valueCode', value: null },
      { name: 'valueUnit', value: null },
      { name: 'organization', value: extractSessionDetails.organization },
      { name: 'recorder', value: recorder },
      { name: 'confidentiality', value: { code: '17621005', name: 'Normal', system: '2.16.840.1.113883.6.96' } as IValueDTO },
    ];
    fieldsToReset.forEach(field => setDefaultValues(form, field.name, field.value));
  }
}

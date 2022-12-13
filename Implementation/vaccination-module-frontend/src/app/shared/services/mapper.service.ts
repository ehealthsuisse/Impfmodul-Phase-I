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
import { TranslateService } from '@ngx-translate/core';
import dayjs from 'dayjs';
import { cloneDeep } from 'lodash';
import { Allergy, IIllnesses, Vaccination } from '../../model';
import { DATE_FORMAT } from '../date';
import { humanToString } from '../function';

@Injectable({ providedIn: 'root' })
export class MapperService {
  translationService = inject(TranslateService);

  all: Allergy[] = [];

  allergyTranslationMapper(records: Allergy[]): Allergy[] {
    let clone = cloneDeep(records);
    clone.forEach(i => {
      i.occurrenceDate = dayjs(i.occurrenceDate).format(DATE_FORMAT);
      i.allergyCode = this.translationService.instant(`ALLERGY_NAMES.${i.allergyCode.code}`);
      i.clinicalStatus = this.translationService.instant(`ALLERGY_CLINICAL_STATUS.${i.clinicalStatus.code}`);
      i.verificationStatus = this.translationService.instant(`ALLERGY_VERIFICATION_STATUS.${i.verificationStatus.code}`);
      i.recorder = humanToString(i.recorder!);
    });
    return clone;
  }

  illnessesTranslateMapper(records: IIllnesses[]): IIllnesses[] {
    let clone = cloneDeep(records);
    clone.forEach(i => {
      i.recordedDate = dayjs(i.recordedDate).format(DATE_FORMAT);

      i.illnessCode = this.translationService.instant(`ILLNESSES_CODE.${i.illnessCode.code}`);
      this.translationService.instant(`ILLNESSES_CODE.` + i.illnessCode.code);
      i.clinicalStatus = this.translationService.instant(`ILLNESS_CLINICAL_STATUS.${i.clinicalStatus.code}`);
      i.verificationStatus = this.translationService.instant(`ILLNESS_VERIFICATION_STATUS.${i.verificationStatus.code}`);
      i.recorder = humanToString(i.recorder!);
    });
    return clone;
  }

  vaccinationTranslateMapper(records: Vaccination[]): Vaccination[] {
    let clone = cloneDeep(records);
    clone.forEach(i => {
      i.occurrenceDate = dayjs(i.occurrenceDate).format(DATE_FORMAT);
      i.vaccineCode = this.translationService.instant(`vaccination-names.` + i.vaccineCode.code);
      i.recorder = humanToString(i.recorder!);
      i.targetDiseases = i.targetDiseases?.map(disease => this.translationService.instant('vaccination-targetdiseases.' + disease.code));
    });
    return clone;
  }

  mapOneVaccination(record: Vaccination): Vaccination {
    let mappedRecord = cloneDeep(record);
    mappedRecord.status = this.translationService.instant(`VACCINE_STATUS.` + record.status.code);

    return mappedRecord;
  }
}

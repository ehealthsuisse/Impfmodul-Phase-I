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
import { TranslateService } from '@ngx-translate/core';
import dayjs from 'dayjs';
import { cloneDeep } from 'lodash';
import { IAdverseEvent, IInfectiousDiseases, IMedicalProblem, IVaccination } from '../../model';
import { DATE_FORMAT } from '../date';
import { humanToString } from '../function';

@Injectable({ providedIn: 'root' })
export class MapperService {
  translateService = inject(TranslateService);

  allergyTranslateMapper(records: IAdverseEvent[]): IAdverseEvent[] {
    let clone = cloneDeep(records);
    clone.sort(this.sortByOccurrenceDate());
    clone.forEach(i => {
      i.occurrenceDate = dayjs(i.occurrenceDate).format(DATE_FORMAT);
      i.code = this.translateService.instant(`ALLERGY_CODE.${i.code.code}`);
      i.clinicalStatus = this.translateService.instant(`ALLERGY_CLINICAL_STATUS.${i.clinicalStatus.code}`);
      i.verificationStatus = this.translateService.instant(`ALLERGY_VERIFICATION_STATUS.${i.verificationStatus.code}`);
      i.recorder = humanToString(i.recorder!);
    });
    return clone;
  }

  illnessesTranslateMapper(records: IInfectiousDiseases[]): IInfectiousDiseases[] {
    let clone = cloneDeep(records);
    clone.sort(this.sortByRecordedDate());
    clone.forEach(i => {
      i.recordedDate = dayjs(i.recordedDate).format(DATE_FORMAT);

      i.illnessCode = this.translateService.instant(`ILLNESSES_CODE.${i.illnessCode.code}`);
      i.clinicalStatus = this.translateService.instant(`ILLNESS_CLINICAL_STATUS.${i.clinicalStatus.code}`);
      i.verificationStatus = this.translateService.instant(`ILLNESS_VERIFICATION_STATUS.${i.verificationStatus.code}`);
      i.recorder = humanToString(i.recorder!);
    });
    return clone;
  }

  problemTranslateMapper(records: IMedicalProblem[]): IMedicalProblem[] {
    let clone = cloneDeep(records);
    clone.sort(this.sortByRecordedDate());
    clone.forEach(i => {
      i.recordedDate = dayjs(i.recordedDate).format(DATE_FORMAT);
      i.medicalProblemCode = this.translateService.instant(`MEDICAL_PROBLEM_CODE.${i.code?.code}`);
      i.clinicalStatus = this.translateService.instant(`MEDICAL_PROBLEM_CLINICAL_STATUS.${i.clinicalStatus?.code}`);
      i.verificationStatus = this.translateService.instant(`MEDICAL_PROBLEM_VERIFICATION_STATUS.${i.verificationStatus?.code}`);
      i.recorder = humanToString(i.recorder!);
    });
    return clone;
  }

  vaccinationTranslateMapper(records: IVaccination[]): IVaccination[] {
    let clone = cloneDeep(records);
    clone.sort(this.sortByOccurrenceDate());
    clone.forEach(i => {
      i.occurrenceDate = dayjs(i.occurrenceDate).format(DATE_FORMAT);
      i.vaccineCode = this.translateService.instant(`vaccination-names.` + i.vaccineCode.code);
      i.recorder = humanToString(i.recorder!);
      i.doseNumber = i.doseNumber + '.';
      i.targetDiseases = i.targetDiseases?.map(disease => this.translateService.instant('vaccination-targetdiseases.' + disease.code));
    });
    return clone;
  }

  mapOneVaccination(record: IVaccination): IVaccination {
    let mappedRecord = cloneDeep(record);
    mappedRecord.status = this.translateService.instant(`VACCINE_STATUS.` + record.status.code);

    return mappedRecord;
  }

  sortByOccurrenceDate(): ((a: IVaccination | IAdverseEvent, b: IVaccination | IAdverseEvent) => number) | undefined {
    return (a, b) => this.sort(a.occurrenceDate, b.occurrenceDate, a.code?.name, b.code?.name, a.createdAt, b.createdAt);
  }

  sortByRecordedDate(): ((a: IInfectiousDiseases | IMedicalProblem, b: IInfectiousDiseases | IMedicalProblem) => number) | undefined {
    return (a, b) => this.sort(a.recordedDate, b.recordedDate, a.code?.name, b.code?.name, a.createdAt, b.createdAt);
  }

  private sort(dateA: string | dayjs.Dayjs, dateB: string | dayjs.Dayjs, codeA: string, codeB: string, createdAtA: dayjs.Dayjs | undefined, createdAtB: dayjs.Dayjs | undefined): number {
    let sort = this.sortByDate(dateA, dateB);

    // fallback to createdAt if occurrenceDate is the same
    if (sort === 0 && createdAtA && createdAtB) {
      if (codeA === codeB) {
        return this.sortByDate(createdAtA, createdAtB);
      } else {  
        return codeA < codeB ? 1 : -1;
      }
    }

    return sort;
  }

  private sortByDate(a: dayjs.Dayjs | string, b: dayjs.Dayjs | string): number {
    let aDay = a;
    let bDay = b;

    if (typeof a === 'string') {
      aDay = dayjs(a);
      bDay = dayjs(b);
    } else {
      aDay = a;
      bDay = b;
    }

    if (aDay.isSame(bDay)) {
      return 0;
    } 

    return aDay.isBefore(bDay) ? 1 : -1;
  }
}

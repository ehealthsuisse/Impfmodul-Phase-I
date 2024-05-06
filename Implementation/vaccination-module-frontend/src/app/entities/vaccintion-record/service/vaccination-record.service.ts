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
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { firstValueFrom, map, Observable, tap } from 'rxjs';
import { ConfigService } from '../../../core/config/config.service';
import { SessionInfoService } from '../../../core/security/session-info.service';
import { IVaccinationRecord } from '../../../model';
import { IValueDTO } from '../../../shared';
import { SpinnerService } from '../../../shared/services/spinner.service';

@Injectable({
  providedIn: 'root',
})
export class VaccinationRecordService {
  resource = ``;
  prefix = 'vaccinationRecord';
  backendVersion?: string;

  constructor(
    private translateService: TranslateService,
    private configService: ConfigService,
    private http: HttpClient,
    private sessionInfoService: SessionInfoService,
    private spinnerService: SpinnerService
  ) {
    this.resource = `${this.prefix}/communityIdentifier/${this.configService.communityId}/oid/${this.sessionInfoService.queryParams.laaoid || this.configService.defaultLaaoid
      }/localId/${this.sessionInfoService.queryParams.lpid || this.configService.defaultLpid}`;
  }

  queryOneRecord(): Observable<IVaccinationRecord> {
    this.spinnerService.show();
    return this.http.get<IVaccinationRecord>(`${this.configService.endpointPrefix}/${this.resource}`).pipe(
      map(record => {
        record.allergies.map((allergy: any) => delete allergy.allergyCode);
        return {
          ...record,
          lang: this.translateService.currentLang,
          author: this.sessionInfoService.author.getValue(),
        };
      }),
      tap(() => this.spinnerService.hide())
    );
  }

  exportPdf(body: IVaccinationRecord): Observable<Blob> {
    this.spinnerService.show();
    return this.http
      .post<Blob>(`${this.configService.endpointPrefix}/vaccinationRecord/exportToPDF`, body, {
        responseType: 'blob' as 'json',
      })
      .pipe(tap(() => this.spinnerService.hide()));
  }

  saveRecord(record: IVaccinationRecord): Observable<IVaccinationRecord> {
    this.spinnerService.show();
    record.author = this.sessionInfoService.author.getValue();
    return this.http
      .post<IVaccinationRecord>(`${this.configService.endpointPrefix}/${this.resource}`, record, {
        responseType: 'json',
      })
      .pipe(tap(() => this.spinnerService.hide()));
  }

  fetchTargetDisease(): Observable<IValueDTO[]> {
    return this.http.get<IValueDTO[]>(`${this.configService.endpointPrefix}/utility/targetDiseases`);
  }

  async getVersion(): Promise<string> {
    if (this.backendVersion) {
      return this.backendVersion;
    }

    this.backendVersion = await firstValueFrom(this.http.get(`${this.configService.endpointPrefix}/utility/backendVersion`, { responseType: 'text' }))
      .catch((error: any) => {
        console.error('Error fetching backend version:', error);
        this.backendVersion = '-';
      }) as string;

    return this.backendVersion;
  }
}

export function filterPatientRecordData(patientRecord: IVaccinationRecord): IVaccinationRecord {
  patientRecord.vaccinations = patientRecord.vaccinations.filter(vaccination => !vaccination.hasErrors);
  patientRecord.medicalProblems = patientRecord.medicalProblems.filter(medicalProblem => !medicalProblem.hasErrors);
  patientRecord.pastIllnesses = patientRecord.pastIllnesses.filter(pastIllness => !pastIllness.hasErrors);
  patientRecord.allergies = patientRecord.allergies.filter(allergy => !allergy.hasErrors);

  return patientRecord;
}

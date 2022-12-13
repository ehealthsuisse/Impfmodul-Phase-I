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
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { map, Observable } from 'rxjs';
import { ApplicationConfigService } from '../../../core';
import { IVaccinationRecord } from '../../../model';
import { SharedDataService } from '../../../shared/services/shared-data.service';

@Injectable({
  providedIn: 'root',
})
export class VaccinationRecordService {
  sharedDataService: SharedDataService = inject(SharedDataService);
  private _vaccinationRecord!: IVaccinationRecord;
  private config: ApplicationConfigService = inject(ApplicationConfigService);
  private translateService: TranslateService = inject(TranslateService);
  private http = inject(HttpClient);
  private resource: string = `vaccinationRecord/communityIdentifier/${this.config.communityId}/oid/${this.sharedDataService.storedData['laaoid']}/localId/${this.sharedDataService.storedData['lpid']}`;

  get vaccinationRecord(): IVaccinationRecord {
    return this._vaccinationRecord;
  }

  set vaccinationRecord(value: IVaccinationRecord) {
    this._vaccinationRecord = value;
  }

  queryOneRecord(): Observable<IVaccinationRecord> {
    return this.http
      .get<IVaccinationRecord>(`${this.config.endpointPrefix}/${this.resource}`)
      .pipe(map((response: IVaccinationRecord) => (this.vaccinationRecord = response)));
  }

  exportPdf(body: IVaccinationRecord): Observable<Blob> {
    let currentLanguage = this.translateService.currentLang;
    body.lang = currentLanguage;
    body.author = {
      firstName: this.sharedDataService.storedData['ufname']!,
      lastName: this.sharedDataService.storedData['ugname']!,
      prefix: this.sharedDataService.storedData['utitle']!,
    };
    return this.http.post<Blob>(`${this.config.endpointPrefix}/vaccinationRecord/exportToPDF`, body, {
      responseType: 'blob' as 'json',
    });
  }

  saveRecord(record: IVaccinationRecord): Observable<IVaccinationRecord> {
    record.author = {
      firstName: this.sharedDataService.storedData['ufname']!,
      lastName: this.sharedDataService.storedData['ugname']!,
      prefix: this.sharedDataService.storedData['utitle']!,
    };
    return this.http.post<IVaccinationRecord>(`${this.config.endpointPrefix}/${this.resource}`, record, {
      responseType: 'json',
    });
  }
}

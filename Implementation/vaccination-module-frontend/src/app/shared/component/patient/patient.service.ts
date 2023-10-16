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
import { Injectable } from '@angular/core';
import { IHumanDTO } from '../../interfaces';
import { BehaviorSubject, Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { ConfigService } from '../../../core/config/config.service';
import { SessionInfoService } from '../../../core/security/session-info.service';

@Injectable({
  providedIn: 'root',
})
export class PatientService {
  resource: string = '';
  patient: BehaviorSubject<IHumanDTO> = new BehaviorSubject<IHumanDTO>({} as IHumanDTO);
  constructor(private httpclient: HttpClient, private configService: ConfigService, private sessionInfoService: SessionInfoService) {
    this.resource = `communityIdentifier/${this.configService.communityId}/oid/${
      this.sessionInfoService.queryParams.laaoid || this.configService.defaultLaaoid
    }/localId/${this.sessionInfoService.queryParams.lpid || this.configService.defaultLpid}`;
  }

  fetchPatientName(): Observable<string> {
    return this.httpclient.get(`${this.configService.endpointPrefix}/vaccinationRecord/${this.resource}/name`, { responseType: 'text' });
  }
}

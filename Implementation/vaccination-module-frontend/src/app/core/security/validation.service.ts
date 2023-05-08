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
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { CryptoJsService } from './crypto-js.service';
import { SessionInfoService } from './session-info.service';
import { ConfigService } from '../config/config.service';
import { ReplaySubject } from 'rxjs';

/**
 *  Service to verifiy initial web call from the portal.
 */
@Injectable({
  providedIn: 'root',
})
export class ValidationService {
  isValidationSuccessful: ReplaySubject<boolean> = new ReplaySubject<boolean>(1);
  constructor(
    private activatedRoute: ActivatedRoute,
    private config: ConfigService,
    private http: HttpClient,
    private router: Router,
    private cryptoService: CryptoJsService,

    private sessionInfoService: SessionInfoService
  ) {}
  validate = (backedUrl: string): boolean => {
    let receivedContent = window.location.href.split('?')[1];
    const queryParams = new URLSearchParams(window.location.search);
    let receivedSignature = queryParams.get('sig')?.replace(/ /g, '+');
    if (receivedSignature) {
      let queryString = receivedContent.substring(0, receivedContent.lastIndexOf('sig') + 4) + receivedSignature;

      this.http.post<boolean>(`${backedUrl}/signature/validate`, queryString).subscribe({
        next: (e: boolean) => {
          this.cryptoService.encryptPortalData(queryString);
          this.cryptoService.encryptValidationStatus(e);
          this.isValidationSuccessful.next(e);
          this.sessionInfoService.initializeSessionInfo();
          this.onValidationSuccess();
        },
        error: () => this.onValidationFailure(),
      });
      return true;
    }
    return false;
  };

  private onValidationSuccess = (): void => {
    this.router.navigateByUrl('/');
  };

  private onValidationFailure = (): void => {
    this.router.navigateByUrl('/error');
  };
}

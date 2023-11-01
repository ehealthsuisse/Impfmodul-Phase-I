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
import { Router } from '@angular/router';
import { ReplaySubject } from 'rxjs';
import { IPortalParameter } from 'src/app/model/portal-parameter';
import { CryptoJsService } from './crypto-js.service';
import { SessionInfoService } from './session-info.service';
import { SignatureService } from './signature.service';

/**
 *  Service to verifiy initial web call from the portal.
 */
@Injectable({
  providedIn: 'root',
})
export class ValidationService {
  isValidationSuccessful: ReplaySubject<boolean> = new ReplaySubject<boolean>(1);
  url: ReplaySubject<string> = new ReplaySubject<string>(1);

  constructor(
    private router: Router,
    private signatureService: SignatureService,
    private cryptoService: CryptoJsService,
    private sessionInfoService: SessionInfoService
  ) {}

  validate = (): void => {
    const queryString = window.location.href.split('?')[1];
    const queryParams = new URLSearchParams(window.location.search);
    let receivedSignature = queryParams.get('sig')?.replace(/ /g, '+');

    if (receivedSignature) {
      this.sessionInfoService.queryParams = {
        principalname: queryParams.get('principalname') ?? '',
        principalid: queryParams.get('principalid') ?? '',
        idp: queryParams.get('idp') ?? '',
        laaoid: queryParams.get('laaoid') ?? '',
        lang: queryParams.get('lang') ?? '',
        lpid: queryParams.get('lpid') ?? '',
        purpose: queryParams.get('purpose') ?? '',
        role: queryParams.get('role') ?? '',
        timestamp: queryParams.get('timestamp') ?? '',
        ufname: queryParams.get('ufname') ?? '',
        ugname: queryParams.get('ugname') ?? '',
        utitle: queryParams.get('utitle') ?? '',
        ugln: queryParams.get('ugln') ?? '',
        sig: queryParams.get('sig') ?? '',
      };

      let query = queryString.substring(0, queryString.lastIndexOf('sig') + 4) + receivedSignature;
      this.signatureService.validateQueryString(query).subscribe({
        next: (isValid: boolean) => {
          if (isValid) {
            this.cleanUp();
            this.cryptoService.encryptPortalData(queryString);
            this.isValidationSuccessful.next(true);
            this.onValidationSuccess();
          } else {
            this.isValidationSuccessful.next(false);
            this.onValidationFailure();
          }
        },
      });
    }
  };

  private onValidationSuccess = (): void => {
    this.router.navigateByUrl('/');
  };

  private onValidationFailure = (): void => {
    // reset query parameters due to invalid login call
    this.sessionInfoService.queryParams = {} as IPortalParameter;
    this.router.navigateByUrl('/error');
  };

  private cleanUp(): void {
    this.cleanUpSessionStorage();
    this.cleanUpLocalStorage();
  }

  private cleanUpSessionStorage = (): void => sessionStorage.removeItem('vaccination-portal-data');
  private cleanUpLocalStorage = (): void => {
    localStorage.removeItem('vaccination-portal-data');
  };
}

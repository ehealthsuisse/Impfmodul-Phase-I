import { SharedDataService } from './../../shared/services/shared-data.service';
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
import { BehaviorSubject } from 'rxjs';
import { IPortalParameter } from '../../model/portal-parameter';
import { IHumanDTO } from '../../shared';
import { CryptoJsService } from './crypto-js.service';

/**
 * Service to receive the parameters from the portal and store them into the session storage.
 */
@Injectable({
  providedIn: 'root',
})
export class SessionInfoService {
  private _queryParams: IPortalParameter = {} as IPortalParameter;
  private _author: BehaviorSubject<IHumanDTO> = new BehaviorSubject<IHumanDTO>({} as IHumanDTO);

  get queryParams(): IPortalParameter {
    return this._queryParams;
  }

  get author(): BehaviorSubject<IHumanDTO> {
    return this._author;
  }

  set author(value: BehaviorSubject<IHumanDTO>) {
    this._author = value;
  }

  constructor(private cryptoService: CryptoJsService, private sharedDataService: SharedDataService) {}

  canValidate(): boolean {
    return this.queryParams.role === 'HCP' || this.queryParams.role === 'ASS';
  }

  initializeSessionInfo(): Promise<void> {
    return new Promise(resolve => {
      this.cryptoService.decryptPortalData();
      if (this.cryptoService.encryptedData) {
        const params = JSON.parse(this.cryptoService.encryptedData);
        for (const [key, value] of new URLSearchParams(params)) {
          this.queryParams[key as keyof IPortalParameter] = decodeURIComponent(value) as string;
        }
      }
      this.author.subscribe({
        next: e => {
          e.lastName = this.queryParams.ufname ?? 'Müller';
          e.firstName = this.queryParams.ugname ?? 'Peter';
          e.prefix = this.queryParams.utitle ?? 'Dr.';
          e.role = this.queryParams.role ?? 'HCP';
          e.lang = this.queryParams.lang ?? 'DE_de';
          e.purpose = this.queryParams.purpose ?? 'NORM';
          e.idp = this.queryParams.idp ?? 'testIDP';
          this.sharedDataService.storedData['role'] = e.role;
        },
      });
      resolve();
    });
  }
}

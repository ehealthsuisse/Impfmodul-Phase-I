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
import { Injectable } from '@angular/core';
import * as CryptoJS from 'crypto-js';

@Injectable({
  providedIn: 'root',
})
export class SharedDataService {
  storedData: any = {};
  storedDataEncrypted: string = '';

  constructor() {
    this.getSessionStorage();
  }

  setSessionStorage(): void {
    if (!this.storedData) {
      sessionStorage.setItem('vaccination-portal-data', '');
      this.storedData = {};
    } else {
      this.storedDataEncrypted = CryptoJS.AES.encrypt(JSON.stringify(this.storedData), 'vaccination-portal-data-encrypt').toString();
      sessionStorage.setItem('vaccination-portal-data', JSON.stringify(this.storedDataEncrypted));
    }
  }

  getSessionStorage(): void {
    if (sessionStorage.getItem('vaccination-portal-data')) {
      this.storedDataEncrypted = CryptoJS.AES.decrypt(
        JSON.parse(sessionStorage.getItem('vaccination-portal-data')!),
        'vaccination-portal-data-encrypt'
      ).toString(CryptoJS.enc.Utf8);
      this.storedData = JSON.parse(this.storedDataEncrypted);
    } else {
      sessionStorage.setItem('vaccination-portal-data', '');
      this.storedData = {};
    }
  }
}

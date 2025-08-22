import { Injectable } from '@angular/core';
import * as CryptoJS from 'crypto-js';
import { IPortalParameter } from '../../model/portal-parameter';

@Injectable({
  providedIn: 'root',
})
export class CryptoJsService {
  encryptedData: string = '';
  storedData: IPortalParameter = {} as IPortalParameter;

  portalSecretKey: string = 'vaccinations-portal-data-encrypt';
  portalKey: string = 'vaccinations-portal-data';
  validationSecretKey: string = 'validation-status-encrypt';
  validationKey: string = 'validation-status';

  encryptPortalData(data: any): void {
    if (!data) {
      localStorage.setItem(this.portalKey, '');
    } else {
      this.encryptedData = CryptoJS.AES.encrypt(JSON.stringify(data), this.portalSecretKey).toString();
      localStorage.setItem(this.portalKey, JSON.stringify(this.encryptedData));
    }
  }

  decryptPortalData(): any {
    if (localStorage.getItem(this.portalKey)) {
      this.encryptedData = CryptoJS.AES.decrypt(JSON.parse(localStorage.getItem(this.portalKey)!), this.portalSecretKey).toString(
        CryptoJS.enc.Utf8
      );
      this.encryptedData.split('&').forEach(pair => {
        const [key, value] = pair.split('=');
        Object.assign(this.storedData, { [key]: value });
      });
    } else {
      localStorage.setItem(this.portalKey, '');
      this.encryptedData = '';
    }
  }

  encryptValidationStatus(validationStatus: boolean): void {
    if (!validationStatus) {
      console.error('Validation status is not defined');
      localStorage.setItem(this.validationKey, '');
    } else {
      const encrypted = CryptoJS.AES.encrypt(JSON.stringify(validationStatus), this.validationSecretKey).toString();
      localStorage.setItem(this.validationKey, JSON.stringify(encrypted));
    }
  }

  decryptValidationStatus(): any {
    if (localStorage.getItem(this.validationKey)) {
      const decryptedBytes = CryptoJS.AES.decrypt(JSON.parse(localStorage.getItem(this.validationKey)!), this.validationSecretKey);
      return JSON.parse(decryptedBytes.toString(CryptoJS.enc.Utf8));
    }
  }
}

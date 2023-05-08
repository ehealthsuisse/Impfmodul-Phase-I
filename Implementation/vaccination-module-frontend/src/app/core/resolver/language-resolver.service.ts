import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { SessionInfoService } from '../security/session-info.service';
import { CryptoJsService } from '../security/crypto-js.service';

@Injectable({ providedIn: 'root' })
export class LanguageResolver implements Resolve<void> {
  constructor(
    private translateService: TranslateService,
    private sessionInfoService: SessionInfoService,
    private cryptoService: CryptoJsService
  ) {}

  resolve(): Promise<void> {
    this.cryptoService.decryptPortalData();
    const defaultLang = 'en';
    const userLang =
      (this.sessionInfoService.queryParams.lang ? this.sessionInfoService.queryParams.lang.toLocaleLowerCase().slice(0, 2) : 'de') ||
      defaultLang;
    console.error('userLang', userLang);
    return this.translateService.use(userLang).toPromise();
  }
}

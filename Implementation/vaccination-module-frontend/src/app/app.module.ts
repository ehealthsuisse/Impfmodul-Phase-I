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
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { inject, LOCALE_ID, NgModule, provideAppInitializer } from '@angular/core';
import { MAT_DATE_LOCALE } from '@angular/material/core';
import { MatDialogModule } from '@angular/material/dialog';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateService } from '@ngx-translate/core';
import { provideNgxWebstorage, withLocalStorage, withNgxWebstorageConfig, withSessionStorage } from 'ngx-webstorage';
import { AppRoutingModule } from './app-routing.module';
import { ErrorInterceptor, FooterComponent, MainComponent, NavbarComponent } from './core';
import { TranslationModule } from './shared';
import { SharedModule } from './shared/shared.module';
import { TitleStrategy } from '@angular/router';
import { CustomPageTitleStrategy } from './shared/language/CustomPageTitleStrategy';

import { ValidationService } from './core/security/validation.service';
import { ConfigService } from './core/config/config.service';
import { SessionInfoService } from './core/security/session-info.service';
import { SamlInterceptor } from './core/interceptor/saml.interceptor';
import { CsrfInterceptor } from './core/interceptor/csrf.interceptor';

export function initializeConfigApp(configService: ConfigService): any {
  return () => configService.initialize();
}

export function initializeApp(appConfigService: ConfigService, validationService: ValidationService) {
  return () => {
    const initConfig = initializeConfigApp(appConfigService);
    return initConfig().then(() => {
      return appConfigService.initialize().then(() => {
        return validationService.validate()!;
      });
    });
  };
}
export function initializeSessionInfo(sessionInfoService: SessionInfoService) {
  return () => sessionInfoService.initializeSessionInfo();
}

@NgModule({
  declarations: [MainComponent],
  bootstrap: [MainComponent],
  imports: [
    SharedModule,
    BrowserModule,
    BrowserAnimationsModule,
    FooterComponent,
    AppRoutingModule,
    NavbarComponent,
    TranslationModule,
    MatDialogModule,
  ],
  providers: [
    { provide: LOCALE_ID, useValue: 'en' },
    { provide: TranslateService },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: SamlInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: CsrfInterceptor, multi: true },
    { provide: TitleStrategy, useClass: CustomPageTitleStrategy },
    {
      provide: MAT_DATE_LOCALE,
      useFactory: (sessionInfoService: SessionInfoService) => sessionInfoService.queryParams.lang,
      deps: [SessionInfoService],
    },
    provideAppInitializer(() => {
      const configService = inject(ConfigService);
      const validationService = inject(ValidationService);
      return initializeApp(configService, validationService)();
    }),
    provideAppInitializer(() => {
      const sessionInfoService = inject(SessionInfoService);
      return initializeSessionInfo(sessionInfoService)();
    }),
    provideHttpClient(withInterceptorsFromDi()),
    provideNgxWebstorage(
      withNgxWebstorageConfig({
        prefix: 'vm',
        separator: '-',
        caseSensitive: true,
      }),
      withSessionStorage(),
      withLocalStorage()
    ),
  ],
})
export class AppModule {}

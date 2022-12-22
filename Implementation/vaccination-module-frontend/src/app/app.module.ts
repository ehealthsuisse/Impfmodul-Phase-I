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
import { HTTP_INTERCEPTORS, HttpClient, HttpClientModule } from '@angular/common/http';
import { inject, LOCALE_ID, NgModule } from '@angular/core';
import { MAT_DATE_LOCALE } from '@angular/material/core';
import { MatDialogModule } from '@angular/material/dialog';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateService } from '@ngx-translate/core';
import { ORDERED_APP_INITIALIZER, ORDERED_APP_PROVIDER } from 'ngx-ordered-initializer';
import { NgxWebstorageModule } from 'ngx-webstorage';
import { catchError, of, tap } from 'rxjs';
import { AppRoutingModule } from './app-routing.module';
import { ApplicationConfigService, FooterComponent, MainComponent, NavbarComponent } from './core';
import './core/config/dayjs';
import { SessionInfoService } from './core/config/session-info.service';
import { TranslationModule } from './shared';
import { SharedModule } from './shared/shared.module';
import { ErrorInterceptor } from './core/config/error.interceptor';

@NgModule({
  declarations: [MainComponent],
  imports: [
    SharedModule,
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    HttpClientModule,
    FooterComponent,
    AppRoutingModule,
    NavbarComponent,
    NgxWebstorageModule.forRoot({ prefix: 'vm', separator: '-', caseSensitive: true }),
    TranslationModule,
    MatDialogModule,
  ],
  bootstrap: [MainComponent],
  providers: [
    { provide: LOCALE_ID, useValue: 'en' },
    { provide: TranslateService },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    { provide: MAT_DATE_LOCALE, useValue: 'fr-CH' },
    {
      // Force an ordered use of the initializer to ensure that the applicationConfigService
      // is initialized before the formOptionService, otherwise the environment variables are undefined due to race conditions.
      provide: ORDERED_APP_INITIALIZER,
      useFactory: () => {
        const appConfigService = inject(ApplicationConfigService);
        const http: HttpClient = inject(HttpClient);
        const setting = require('../assets/config.json');
        return () => {
          return new Promise(resolve => {
            if (process.env['NODE_ENV'] === 'production') {
              http
                .get('/assets/config.json')
                .pipe(
                  tap((config: any) => {
                    appConfigService.endpointPrefix = config.backendURL;
                    appConfigService.communityId = config.communityId;
                    resolve(true);
                  }),
                  catchError(() => {
                    appConfigService.endpointPrefix = setting.backendURL;
                    appConfigService.communityId = setting.communityId;
                    resolve(true);
                    return of(null);
                  })
                )
                .subscribe();
            } else {
              appConfigService.endpointPrefix = 'http://localhost:8080';
              appConfigService.communityId = setting.communityId;
              resolve(true);
            }
          });
        };
      },
      multi: true,
    },
    {
      provide: ORDERED_APP_INITIALIZER,
      useFactory: (sessionInfo: SessionInfoService) => sessionInfo.sessionInfo(),
      deps: [SessionInfoService],
      multi: true,
    },
    ORDERED_APP_PROVIDER,
  ],
})
export class AppModule {}

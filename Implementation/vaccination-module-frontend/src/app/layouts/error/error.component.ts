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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { SessionStorageService } from 'ngx-webstorage';
import { Subscription } from 'rxjs';
import { ConfigService } from 'src/app/core/config/config.service';
import { SharedLibsModule } from '../../shared/shared-libs.module';

/**
 * Componete is used to display the error content which is forwarded to in case of an invalid web request.
 */
@Component({
  selector: 'vm-error',
  standalone: true,
  templateUrl: './error.component.html',
  styles: [
    '.error-wrapper { height: 100vh; width: 100vw; display: grid; place-items: center; position: absolute; top: 0px; left: 0px }',
    '.content { display: grid; place-items: center; }',
  ],
  imports: [SharedLibsModule],
})
export class ErrorComponent implements OnInit, OnDestroy {
  errorMessage?: string;
  errorKey?: string;
  errorCode?: number;
  langChangeSubscription?: Subscription;
  textColor?: string;

  constructor(
    private translateService: TranslateService,
    private route: ActivatedRoute,
    private sessionStorageService: SessionStorageService,
    private configService: ConfigService
  ) {
    this.selectLanguage();
  }

  ngOnInit(): void {
    this.route.data.subscribe(routeData => {
      const errorMessage = routeData['errorMessage'];
      this.textColor = errorMessage === 'GLOBAL.LOGOUT' ? 'black' : 'red';
      if (errorMessage) {
        this.errorKey = errorMessage;
        this.errorCode = routeData['errorCode'];
        this.getErrorMessageTranslation();
      }
    });

    if (this.errorKey === 'GLOBAL.LOGOUT' && this.configService.logoutForwardUrl) {
      this.forwardToUrl(this.configService.logoutForwardUrl);
    }
  }

  forwardToUrl(url: string): void {
    setTimeout(() => {
      window.location.href = url;
    }, 2000);
  }

  ngOnDestroy(): void {
    if (this.langChangeSubscription) {
      this.langChangeSubscription.unsubscribe();
    }
  }

  private getErrorMessageTranslation(): void {
    this.errorMessage = '';
    if (this.errorKey) {
      this.translateService.get(this.errorKey).subscribe(translatedErrorMessage => {
        this.errorMessage = translatedErrorMessage;
      });
    }
  }

  private selectLanguage(): void {
    const language = this.sessionStorageService.retrieve('lo');
    if (language !== null) {
      // Selected language from the drop-down menu
      this.translateService.use(language);
    } else {
      // Language selected on Portal
      this.langChangeSubscription = this.translateService.onLangChange.subscribe(() => this.getErrorMessageTranslation());
    }
  }
}

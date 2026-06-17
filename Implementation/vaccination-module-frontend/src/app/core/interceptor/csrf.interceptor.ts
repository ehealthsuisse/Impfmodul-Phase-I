/**
 * Copyright (c) 2024 eHealth Suisse
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
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpXsrfTokenExtractor } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, take, timer } from 'rxjs';
import { DialogService } from 'src/app/shared';
import { SpinnerService } from 'src/app/shared/services/spinner.service';
import { ConfigService } from '../config/config.service';
import { SessionInfoService } from '../security/session-info.service';

@Injectable()
export class CsrfInterceptor implements HttpInterceptor {
  errorDialogOpen: boolean = false;
  constructor(
    private tokenExtractor: HttpXsrfTokenExtractor,
    private sessionInfoService: SessionInfoService,
    private dialogService: DialogService,
    private configService: ConfigService,
    private spinnerService: SpinnerService
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const headerName = 'X-XSRF-TOKEN';
    const csrfToken = this.tokenExtractor.getToken() as string;
    let modifiedRequest = request;

    if (csrfToken) {
      this.sessionInfoService.csrfToken = csrfToken;
    }

    let originalOrModifiedHeaders = request.headers;
    if (!!this.sessionInfoService.csrfToken) {
      originalOrModifiedHeaders = originalOrModifiedHeaders.set(headerName, this.sessionInfoService.csrfToken);
    }

    this.checkValidSession(request);
    modifiedRequest = request.clone({
      headers: originalOrModifiedHeaders,
      withCredentials: true,
    });
    return next.handle(modifiedRequest);
  }

  /**
   * In the production environment, it can happen that the local storage is removed when the portal session expires.
   * This will lead to an unexpected scenario which will make the frontend thing that we are in development mode.
   * This method ensures that if we are not in development mode, the frontend should never send out default parameters to the backend.
   */
  private checkValidSession(request: HttpRequest<unknown>): void {
    const isProd = !this.configService.canActivate;
    if (isProd && request.url.includes(this.configService.defaultLaaoid)) {
      if (!this.errorDialogOpen) {
        this.dialogService.openDialog('GLOBAL.ERROR', 'GLOBAL.DISCONNECT', true, {}, { showOk: true });
        this.errorDialogOpen = true;
        // use time to open dialog only once even if multiple errors occur within 1 second.
        timer(1000)
          .pipe(take(1))
          .subscribe(() => {
            this.errorDialogOpen = false;
          });
        this.spinnerService.hide();
      }
      throw new Error('GLOBAL.DISCONNECT');
    }
  }
}

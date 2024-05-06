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
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, take, throwError, timer } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { DialogService } from '../../shared';
import { SpinnerService } from '../../shared/services/spinner.service';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  private errorDialogOpen = false;

  constructor(public spinnerService: SpinnerService, private dialogService: DialogService) { }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      catchError(err => {
        if (err.status >= 400) {
          if (!this.errorDialogOpen) {
            const error = err.status === 422 ? 'GLOBAL.DISCONNECT' : (err.error || 'GLOBAL.UNEXPECTED_ERROR');
            this.dialogService.openDialog('GLOBAL.ERROR', error, true, {}, { showOk: true });
            this.errorDialogOpen = true;
            // use time to open dialog only once even if multiple errors occur within 1 second.
            timer(1000)
              .pipe(take(1))
              .subscribe(() => {
                this.errorDialogOpen = false;
              });
          }
        }
        this.spinnerService.hide();
        return throwError(err);
      })
    );
  }
}

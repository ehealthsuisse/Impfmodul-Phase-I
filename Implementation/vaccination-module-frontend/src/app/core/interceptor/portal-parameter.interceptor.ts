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
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SessionInfoService } from '../security/session-info.service';

@Injectable()
export class PortalParameterInterceptor implements HttpInterceptor {
  constructor(private sessionInfoService: SessionInfoService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const modifiedRequest = request.clone({
      setHeaders: {
        idp: this.sessionInfoService.queryParams.idp || '',
        role: this.sessionInfoService.queryParams.role || 'HCP',
        purpose: this.sessionInfoService.queryParams.purpose || 'NORM',
        ugln: this.sessionInfoService.queryParams.ugln || '',
        principalid: this.sessionInfoService.queryParams.principalid || '',
        utitle: this.sessionInfoService.queryParams.utitle || 'Dr.',
        ugname: this.sessionInfoService.queryParams.ugname || 'Peter',
        ufname: this.sessionInfoService.queryParams.ufname || 'Müller',
        principalname: this.sessionInfoService.queryParams.principalname || '',
        laaoid: this.sessionInfoService.queryParams.laaoid || '',
      },
    });
    return next.handle(modifiedRequest);
  }
}

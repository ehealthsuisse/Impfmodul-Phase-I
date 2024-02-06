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
import { Observable } from 'rxjs';
import { IPortalParameter } from '../../model/portal-parameter';
import { SessionInfoService } from '../security/session-info.service';

@Injectable()
export class PortalParameterInterceptor implements HttpInterceptor {
  constructor(private sessionInfoService: SessionInfoService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const modifiedHeaders: { [header: string]: string | string[] } = {};
    const queryParams = this.sessionInfoService.queryParams;

    const optionalParameters: Array<keyof IPortalParameter> = ['ugln', 'principalid', 'utitle', 'principalname', 'organization'];

    Object.entries(queryParams).forEach(([key, value]) => {
      if (!optionalParameters.includes(key as keyof IPortalParameter) || value) {
        if (key !== 'sig') {
          modifiedHeaders[`${key}`] = value;
        }
      }
    });

    const modifiedRequest = request.clone({
      setHeaders: modifiedHeaders,
      withCredentials: true,
    });

    return next.handle(modifiedRequest);
  }
}

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
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';

@Injectable()
export class SamlInterceptor implements HttpInterceptor {
  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      tap((event: HttpEvent<any>) => {
        if (event instanceof HttpResponse) {
          let requestBody: string = JSON.stringify(event.body);
          if (this.isFirstSamlRequest(requestBody)) {
            let formHtml = requestBody.slice(requestBody.indexOf('<form'), requestBody.indexOf('</form>') + 7);
            // replace so URL can be read and evaluated
            formHtml = formHtml.replace(/\\"/g, '"');
            this.injectForm(formHtml);
          }
        }
      })
    );
  }

  private isFirstSamlRequest(requestBody: string): boolean {
    const constainsSAMLForm = requestBody.includes('<form');
    const isAlreadyInjected = document.body.innerHTML.includes('SAMLRequest');

    return constainsSAMLForm && !isAlreadyInjected;
  }

  private injectForm(formHtml: string): void {
    document.body.insertAdjacentHTML('beforeend', formHtml);
    const form = document.querySelector('form') as HTMLFormElement;
    form.submit();
  }
}

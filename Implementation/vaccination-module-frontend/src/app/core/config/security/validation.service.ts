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
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApplicationConfigService } from '../application-config.service';
import { ActivatedRoute, Router } from '@angular/router';

/**
 *  Service to verifiy initial web call from the portal.
 */
@Injectable({
  providedIn: 'root',
})
export class ValidationService {
  private http = inject(HttpClient);
  private config = inject(ApplicationConfigService);
  private activatedRoute = inject(ActivatedRoute);
  private router: Router = inject(Router);

  validate = (): any => {
    return () => {
      return new Promise(resolve => {
        this.validateSignature();
        resolve(true);
      });
    };
  };

  validateSignature = (): void => {
    let receivedContent = window.location.href.split('?')[1];
    this.activatedRoute.queryParams.subscribe(params => {
      let receivedSignature = params['sig'];
      if (receivedSignature) {
        receivedSignature = receivedSignature.replace(/ /g, '+');
        let queryString = receivedContent.substring(0, receivedContent.lastIndexOf('sig') + 4) + receivedSignature;
        this.http.post<boolean>(`${this.config.endpointPrefix}/signature/validate`, queryString).subscribe({
          next: res => {
            if (res) {
              this.router.navigateByUrl('/');
            }
          },
          error: () => this.router.navigateByUrl('/error'),
        });
      }
    });
  };
}

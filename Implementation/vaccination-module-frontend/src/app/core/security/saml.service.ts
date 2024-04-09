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
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, shareReplay, tap } from 'rxjs';
import { ConfigService } from '../config/config.service';
import { SessionInfoService } from './session-info.service';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class SamlService {
  isSsoRedirectCompleted: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(
    sessionStorage.getItem('isSsoRedirectCompleted') === 'true'
  );

  private _authStateSubject = new BehaviorSubject<'not-started' | 'in-progress' | 'completed'>('not-started');

  constructor(
    private http: HttpClient,
    private sessionInfoService: SessionInfoService,
    private configService: ConfigService,
    private router: Router
  ) {}

  isAuthenticated = (): Observable<boolean> => {
    if (this.isSsoRedirectCompleted.getValue()) {
      return this.isSsoRedirectCompleted.asObservable();
    }
    return this.http.get<boolean>(this.configService.endpointPrefix + '/saml/isAuthenticated').pipe(
      shareReplay(1),
      tap(isAuthenticated => isAuthenticated && this.isSsoRedirectCompleted.next(true))
    );
  };

  samlLogin(): Observable<string> {
    this.authStateSubject.next('in-progress');
    const headers = new HttpHeaders({ idp: this.sessionInfoService.queryParams.idp });

    return this.http.get(this.configService.endpointPrefix + '/saml/login', { headers, responseType: 'text' }).pipe(
      tap(() => this.authStateSubject.next('completed')),
      shareReplay(1)
    );
  }

  ssoArtifactRedirect = (samlArtifact: string, idpIdentifier: string): Observable<any> => {
    const params = new HttpParams({ fromObject: { SAMLart: samlArtifact, RelayState: idpIdentifier } });
    return this.http.get(`${this.configService.endpointPrefix}/saml/ssoNew`, { params, observe: 'response' });
  };

  // Although the local logout is not using any saml implementation, it was put here to not open yet another service.
  logout(): void {
    this.http.post(this.configService.endpointPrefix + '/logout', {}).subscribe(() => {
      this.isSsoRedirectCompleted.next(false);
      this.router.navigate(['/logout']);
    });
  }

  get authStateSubject(): BehaviorSubject<'not-started' | 'in-progress' | 'completed'> {
    return this._authStateSubject;
  }
}

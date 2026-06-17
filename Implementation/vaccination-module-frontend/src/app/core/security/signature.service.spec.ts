/**
 * Copyright (c) 2026 eHealth Suisse
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
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ConfigService } from '../config/config.service';
import { SignatureService } from './signature.service';

describe('SignatureService', () => {
  let service: SignatureService;
  let httpMock: HttpTestingController;
  let configService: Pick<ConfigService, 'endpointPrefix' | 'frontendHost'>;

  beforeEach(() => {
    configService = {
      endpointPrefix: '',
      frontendHost: '',
    };

    TestBed.configureTestingModule({
      providers: [
        SignatureService,
        { provide: ConfigService, useValue: configService },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(SignatureService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should send configured frontend host', () => {
    configService.endpointPrefix = 'https://master-vaccination-module-backend.apps.ocp4.innershift.sodigital.io';
    configService.frontendHost = 'custom-frontend.example.ch';

    service.validateQueryString('signed-query').subscribe();

    const req = httpMock.expectOne('https://master-vaccination-module-backend.apps.ocp4.innershift.sodigital.io/signature/validate');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      queryString: 'signed-query',
      frontendHost: 'custom-frontend.example.ch',
    });

    req.flush(true);
  });
});

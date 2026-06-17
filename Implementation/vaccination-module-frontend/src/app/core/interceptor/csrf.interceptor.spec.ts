/**
 * Copyright (c) 2025 eHealth Suisse
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
import { TestBed } from '@angular/core/testing';
import { HttpRequest, HttpHandler, HttpXsrfTokenExtractor, HttpEvent } from '@angular/common/http';
import { of } from 'rxjs';
import { CsrfInterceptor } from './csrf.interceptor';

describe('CsrfInterceptor', () => {
  let interceptor: CsrfInterceptor;
  let mockTokenExtractor: jasmine.SpyObj<HttpXsrfTokenExtractor>;
  let mockSessionInfoService: any;
  let mockDialogService: any;
  let mockConfigService: any;
  let mockSpinnerService: any;
  let mockHandler: jasmine.SpyObj<HttpHandler>;

  beforeEach(() => {
    mockTokenExtractor = jasmine.createSpyObj('HttpXsrfTokenExtractor', ['getToken']);
    mockSessionInfoService = { csrfToken: null };
    mockDialogService = { openDialog: jasmine.createSpy('openDialog') };
    mockConfigService = { canActivate: false, defaultLaaoid: 'default' };
    mockSpinnerService = { hide: jasmine.createSpy('hide') };
    mockHandler = jasmine.createSpyObj('HttpHandler', ['handle']);

    TestBed.configureTestingModule({
      providers: [
        CsrfInterceptor,
        { provide: HttpXsrfTokenExtractor, useValue: mockTokenExtractor },
        { provide: 'SessionInfoService', useValue: mockSessionInfoService },
        { provide: 'DialogService', useValue: mockDialogService },
        { provide: 'ConfigService', useValue: mockConfigService },
        { provide: 'SpinnerService', useValue: mockSpinnerService },
      ],
    });

    interceptor = new CsrfInterceptor(mockTokenExtractor, mockSessionInfoService, mockDialogService, mockConfigService, mockSpinnerService);
  });

  it('should add X-XSRF-TOKEN header if token exists', () => {
    mockTokenExtractor.getToken.and.returnValue('test-token');
    const req = new HttpRequest('GET', '/api/test');
    mockHandler.handle.and.returnValue(of({} as HttpEvent<any>));

    interceptor.intercept(req, mockHandler).subscribe();

    expect(mockSessionInfoService.csrfToken).toBe('test-token');
    const calledReq = mockHandler.handle.calls.mostRecent().args[0];
    expect(calledReq.headers.get('X-XSRF-TOKEN')).toBe('test-token');
    expect(calledReq.withCredentials).toBeTrue();
  });

  it('should not add X-XSRF-TOKEN header if token does not exist', () => {
    mockTokenExtractor.getToken.and.returnValue(null);
    const req = new HttpRequest('GET', '/api/test');
    mockHandler.handle.and.returnValue(of({} as HttpEvent<any>));

    interceptor.intercept(req, mockHandler).subscribe();

    const calledReq = mockHandler.handle.calls.mostRecent().args[0];
    expect(calledReq.headers.has('X-XSRF-TOKEN')).toBeFalse();
  });

  it('should call checkValidSession and throw error if invalid session in prod', () => {
    mockTokenExtractor.getToken.and.returnValue('token');
    mockConfigService.canActivate = false; // isProd = true
    mockConfigService.defaultLaaoid = 'default';
    const req = new HttpRequest('GET', '/api/test?laaoid=default');
    mockHandler.handle.and.returnValue(of({} as HttpEvent<any>));

    expect(() => interceptor.intercept(req, mockHandler)).toThrowError('GLOBAL.DISCONNECT');
    expect(mockDialogService.openDialog).toHaveBeenCalled();
    expect(mockSpinnerService.hide).toHaveBeenCalled();
  });

  it('should not throw error if not in prod', () => {
    mockTokenExtractor.getToken.and.returnValue('token');
    mockConfigService.canActivate = true; // isProd = false
    const req = new HttpRequest('GET', '/api/test?laaoid=default');
    mockHandler.handle.and.returnValue(of({} as HttpEvent<any>));

    expect(() => interceptor.intercept(req, mockHandler)).not.toThrow();
  });
});

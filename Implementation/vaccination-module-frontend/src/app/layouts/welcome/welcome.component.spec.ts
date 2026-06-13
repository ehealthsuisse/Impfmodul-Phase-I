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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { WelcomeComponent } from './welcome.component';
import { SamlService } from '../../core/security/saml.service';
import { SpinnerService } from '../../shared/services/spinner.service';

describe('WelcomeComponent', () => {
  let component: WelcomeComponent;
  let fixture: ComponentFixture<WelcomeComponent>;
  let samlServiceMock: any;
  let routerMock: any;
  let spinnerServiceMock: any;

  beforeEach(() => {
    samlServiceMock = {
      authStateSubject: new Subject<string>(),
      samlLogin: jasmine.createSpy('samlLogin').and.returnValue(of('some-url')),
    };
    routerMock = { navigateByUrl: jasmine.createSpy('navigateByUrl') };
    spinnerServiceMock = { show: jasmine.createSpy('show') };

    spyOn(sessionStorage, 'removeItem');
    spyOn(localStorage, 'removeItem');

    TestBed.configureTestingModule({
      imports: [WelcomeComponent],
      providers: [
        { provide: SamlService, useValue: samlServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: SpinnerService, useValue: spinnerServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WelcomeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should clean up session and local storage on init', () => {
    fixture.detectChanges();
    expect(sessionStorage.removeItem).toHaveBeenCalledWith('vaccination-portal-data');
    expect(localStorage.removeItem).toHaveBeenCalledWith('vaccination-portal-data');
    expect(localStorage.removeItem).toHaveBeenCalledWith('validation-status');
  });

  it('should show spinner and call samlLogin if authState is "not-started"', () => {
    fixture.detectChanges();
    samlServiceMock.authStateSubject.next('not-started');
    expect(spinnerServiceMock.show).toHaveBeenCalled();
    expect(samlServiceMock.samlLogin).toHaveBeenCalled();
  });

  it('should redirect to vaccination-record if samlLogin response is not localhost or dev', () => {
    samlServiceMock.samlLogin.and.returnValue(of('https://prod-url.com'));
    fixture.detectChanges();
    samlServiceMock.authStateSubject.next('not-started');
    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/vaccination-record');
  });

  it('should redirect to samlLogin response if it contains "localhost"', () => {
    spyOn(component, 'redirectTo');
    samlServiceMock.samlLogin.and.returnValue(of('http://localhost:4200/redirect'));
    fixture.detectChanges();
    samlServiceMock.authStateSubject.next('not-started');
    expect(component.redirectTo).toHaveBeenCalledWith('http://localhost:4200/redirect');
  });

  it('should redirect to samlLogin response if it contains dev environment', () => {
    spyOn(component, 'redirectTo');
    samlServiceMock.samlLogin.and.returnValue(of('https://apps.ocp4.innershift.sodigital.io/redirect'));
    fixture.detectChanges();
    samlServiceMock.authStateSubject.next('not-started');
    expect(component.redirectTo).toHaveBeenCalledWith('https://apps.ocp4.innershift.sodigital.io/redirect');
  });

  it('should navigate to /access-denied on samlLogin error', () => {
    samlServiceMock.samlLogin.and.returnValue(throwError(() => new Error('fail')));
    fixture.detectChanges();
    samlServiceMock.authStateSubject.next('not-started');
    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/access-denied');
  });

  it('cleanUpSessionStorage should remove session key', () => {
    component.cleanUpSessionStorage();
    expect(sessionStorage.removeItem).toHaveBeenCalledWith('vaccination-portal-data');
  });

  it('cleanUpLocalStorage should remove local keys', () => {
    component.cleanUpLocalStorage();
    expect(localStorage.removeItem).toHaveBeenCalledWith('vaccination-portal-data');
    expect(localStorage.removeItem).toHaveBeenCalledWith('validation-status');
  });
});

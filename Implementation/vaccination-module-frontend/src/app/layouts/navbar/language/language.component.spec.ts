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
import { LanguageComponent } from './language.component';

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SessionInfoService } from '../../../core/security/session-info.service';
import { SessionStorageService } from 'ngx-webstorage';
import { TranslateService } from '@ngx-translate/core';
import { Pipe, PipeTransform } from '@angular/core';
import { of } from 'rxjs';
import { CommonModule, NgClass, UpperCasePipe } from '@angular/common';

@Pipe({ name: 'translate', standalone: true })
class MockTranslatePipe implements PipeTransform {
  transform(value: any): any {
    return value;
  }
}

class MockSessionInfoService {
  queryParams: any = {};
  // eslint-disable-next-line unused-imports/no-unused-vars
  changeLanguage(lang: string): void {}
}
class MockSessionStorageService {
  // eslint-disable-next-line unused-imports/no-unused-vars
  store(key: string, value: any): void {}
}
class MockTranslateService {
  use = jasmine.createSpy('use');
  instant = jasmine.createSpy('instant').and.callFake((key: string) => key);
  get = jasmine.createSpy('get').and.callFake((key: string) => of(key || ''));
}

describe('LanguageComponent', () => {
  let component: LanguageComponent;
  let fixture: ComponentFixture<LanguageComponent>;
  let sessionInfoService: MockSessionInfoService;
  let translateService: MockTranslateService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule, LanguageComponent, MockTranslatePipe],
      providers: [
        { provide: SessionInfoService, useClass: MockSessionInfoService },
        { provide: SessionStorageService, useClass: MockSessionStorageService },
        { provide: TranslateService, useClass: MockTranslateService },
      ],
    })
      .overrideComponent(LanguageComponent, {
        set: { imports: [MockTranslatePipe, NgClass, UpperCasePipe] },
      })
      .compileComponents();

    TestBed.overrideProvider(TranslateService, { useValue: new MockTranslateService() });

    fixture = TestBed.createComponent(LanguageComponent);
    component = fixture.componentInstance;
    sessionInfoService = TestBed.inject(SessionInfoService) as any;
    translateService = TestBed.inject(TranslateService) as any;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle dropdownOpen on onLanguageClick', () => {
    expect(component.dropdownOpen).toBeFalse();
    component.onLanguageClick();
    expect(component.dropdownOpen).toBeTrue();
    component.onLanguageClick();
    expect(component.dropdownOpen).toBeFalse();
  });

  it('should set currentLanguage and call changeLang on changeLanguage', () => {
    const spy = spyOn<any>(component, 'changeLanguage').and.callThrough();
    component.currentLanguage = 'de';
    component.changeLanguage('fr', undefined);
    expect(component.currentLanguage).toBe('fr');
    expect(spy).toHaveBeenCalledWith('fr', undefined);
  });

  it('should close dropdown if click is outside', () => {
    component.dropdownOpen = true;
    component.dropdownElement = { nativeElement: { contains: () => false } } as any;
    component.closeDropdown({ target: null } as any);
    expect(component.dropdownOpen).toBeFalse();
  });

  it('should not close dropdown if click is inside', () => {
    component.dropdownOpen = true;
    component.dropdownElement = { nativeElement: { contains: () => true } } as any;
    component.closeDropdown({ target: null } as any);
    expect(component.dropdownOpen).toBeTrue();
  });

  it('should set language from queryParams if valid', () => {
    sessionInfoService.queryParams = { lang: 'fr' };
    component.languages = ['de', 'fr', 'it'];
    component.ngOnInit();
    expect(component.currentLanguage).toBe('fr');
    expect(translateService.use).toHaveBeenCalledWith('fr');
  });

  it('should set language to de if lang is rm', () => {
    sessionInfoService.queryParams = { lang: 'rm' };
    component.languages = ['de', 'fr', 'it'];
    component.ngOnInit();
    expect(component.currentLanguage).toBe('de');
    expect(translateService.use).toHaveBeenCalledWith('de');
  });
});

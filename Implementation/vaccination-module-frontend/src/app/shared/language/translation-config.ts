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
import { HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';
import { MissingTranslationHandler, MissingTranslationHandlerParams, TranslateLoader, TranslateService } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { SessionStorageService } from 'ngx-webstorage';
import { map, Observable, startWith } from 'rxjs';
import { SessionInfoService } from '../../core/security/session-info.service';

export class MissingTranslationHandlerImpl implements MissingTranslationHandler {
  handle(params: MissingTranslationHandlerParams): string {
    const key = params.key;
    return `${translationNotFoundMessage}[${key}]`;
  }
}

export function translatePartialLoader(http: HttpClient): TranslateLoader {
  const I18N_HASH: string = '';
  return new TranslateHttpLoader(http, 'i18n/', `.json?_generated_hash=${I18N_HASH}`);
}

export function missingTranslationHandler(): MissingTranslationHandler {
  return new MissingTranslationHandlerImpl();
}

export function trackLangChange(): Observable<string> {
  return inject(TranslateService).onLangChange.pipe(
    map(change => change.lang),
    startWith('DE')
  );
}

export function changeLang(
  languageKey: string,
  sessionInfoService: SessionInfoService,
  translateService: TranslateService,
  sessionStorageService: SessionStorageService,
  currentLanguage: string
): string {
  if (languageKey !== currentLanguage) {
    sessionInfoService.queryParams.lang = languageKey.toLowerCase();
    sessionStorageService.store('locale'.slice(0, 2).toLocaleLowerCase(), languageKey);
    translateService.use(languageKey.slice(0, 2).toLocaleLowerCase());
    currentLanguage = languageKey.slice(0, 2);
  }

  return currentLanguage;
}

export const translationNotFoundMessage = 'translation-not-found';

export const LANGUAGES: string[] = ['de', 'en', 'fr', 'it'];
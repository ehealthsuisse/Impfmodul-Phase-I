/**
 * Copyright (c) 2024 eHealth Suisse
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
import { TranslateLoader } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Custom loader to handle missing translations and fallback to default language
 */
export class CustomTranslateLoader implements TranslateLoader {
  constructor(private http: HttpClient, private prefix: string, private suffix: string) {}

  // Intercept the language parameter and change it
  getTranslation(lang: string): Observable<any> {
    const I18N_HASH: string = '';
    const modifiedLang = this.modifyLanguage(lang);
    this.prefix = 'i18n/';
    this.suffix = `.json?_generated_hash=${I18N_HASH}`;

    return this.http.get(`${this.prefix}${modifiedLang}${this.suffix}`);
  }

  // Custom logic to modify the language parameter
  modifyLanguage(lang: string): string {
    if (lang === 'rm') {
      return 'de';
    }
    return lang;
  }
}

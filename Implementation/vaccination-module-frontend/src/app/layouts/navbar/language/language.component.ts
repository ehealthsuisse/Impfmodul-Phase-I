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
import { ChangeDetectionStrategy, Component, ElementRef, HostListener, inject, OnInit, ViewChild } from '@angular/core';
import { changeLang, LANGUAGES } from '../../../shared';
import { SessionInfoService } from '../../../core/security/session-info.service';
import { BreakPointSensorComponent } from '../../../shared/component/break-point-sensor/break-point-sensor.component';
import { SessionStorageService } from 'ngx-webstorage';
import { SharedLibsModule } from '../../../shared/shared-libs.module';

@Component({
  selector: 'vm-language',
  standalone: true,
  imports: [SharedLibsModule],
  templateUrl: './language.component.html',
  styleUrls: ['./language.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LanguageComponent extends BreakPointSensorComponent implements OnInit {
  languages = LANGUAGES;
  currentLanguage!: string;
  sessionInfoService: SessionInfoService = inject(SessionInfoService);
  sessionStorageService: SessionStorageService = inject(SessionStorageService);

  @ViewChild('dropdown') dropdownElement!: ElementRef;
  private _dropdownOpen = false;

  changeLanguage = (languageKey: string, event?: MouseEvent): void => {
    changeLang(languageKey, this.sessionInfoService, this.translateService, this.sessionStorageService, this.currentLanguage);
    this.currentLanguage = languageKey;
    if (event) {
      event.stopPropagation();
      this.dropdownOpen = false;
    }
  };

  ngOnInit(): void {
    let language = 'de';
    if (this.sessionInfoService.queryParams.lang) {
      const lang = this.sessionInfoService.queryParams.lang?.toLocaleLowerCase().slice(0, 2)!;
      if (this.languages.includes(lang)) {
        language = lang;
      } else if (lang === 'rm') {
        language = 'de';
      } else {
        this.dialogService.openDialog(
          this.translateService.instant('GLOBAL.ERROR'),
          this.translateService.instant('GLOBAL.UNSUPPORTED_LANGUAGE', { lang: `<bold style="color:red">${lang}</bold>`.toUpperCase() })
        );
      }
    }
    this.translateService.use(language);
    this.currentLanguage = language;
  }

  onLanguageClick(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  @HostListener('document:click', ['$event'])
  closeDropdown(event: Event): void {
    if (!this.dropdownElement.nativeElement.contains(event.target)) {
      this.dropdownOpen = false;
    }
  }

  get dropdownOpen(): boolean {
    return this._dropdownOpen;
  }

  set dropdownOpen(value: boolean) {
    this._dropdownOpen = value;
  }
}

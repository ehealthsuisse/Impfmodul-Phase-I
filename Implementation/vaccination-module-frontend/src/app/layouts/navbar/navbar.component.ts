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
import { Component, inject, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { SessionStorageService } from 'ngx-webstorage';
import { SharedLibsModule } from '../../shared/shared-libs.module';
import { LANGUAGES } from '../../shared';
import { Router, RouterModule } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSidenavModule } from '@angular/material/sidenav';
import { FlexLayoutModule } from '@angular/flex-layout';
import { ApplicationConfigService } from '../../core';
import { map } from 'rxjs';
import { SharedDataService } from '../../shared/services/shared-data.service';

/**
 * Used to display the top bar.
 */
@Component({
  standalone: true,
  selector: 'vm-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  imports: [
    SharedLibsModule,
    RouterModule,
    MatListModule,
    MatToolbarModule,
    MatCardModule,
    MatTabsModule,
    MatSidenavModule,
    FlexLayoutModule,
  ],
})
export class NavbarComponent implements OnInit {
  languages = LANGUAGES;
  currentLanguage!: string;
  translateService: TranslateService = inject(TranslateService);
  sessionStorageService: SessionStorageService = inject(SessionStorageService);
  router: Router = inject(Router);
  appConfig = inject(ApplicationConfigService);
  sharedDataService: SharedDataService = inject(SharedDataService);

  prefix = '';
  firstName = '';
  lastName = '';

  currentLang = this.translateService.onLangChange.pipe(map(change => change.lang));

  data: Map<string, string> = new Map<string, string>();

  showLangs: boolean = false;
  showMenu: boolean = false;

  ngOnInit(): void {
    this.translateService.use(this.sharedDataService.storedData['lang']?.toLocaleLowerCase().slice(0, 2)! || 'de');
    this.getUsername();
  }

  getUsername(): void {
    const utitle = this.sharedDataService.storedData['utitle'];
    const ufname = this.sharedDataService.storedData['ufname'];
    const ugname = this.sharedDataService.storedData['ugname'];

    this.prefix = !utitle || utitle === 'null' ? '' : utitle;
    this.firstName = !ufname || ufname === 'null' ? '' : ufname;
    this.lastName = !ugname || ugname === 'null' ? '' : ugname;
  }

  changeLanguage = (languageKey: string): void => {
    this.sessionStorageService.store('locale'.slice(0, 2).toLocaleLowerCase(), languageKey);
    this.translateService.use(languageKey.slice(0, 2).toLocaleLowerCase());
    this.currentLanguage = languageKey.slice(0, 2);
  };

  toggleMenu(): void {
    this.showMenu = !this.showMenu;
  }
  toggleLangs(): void {
    this.showLangs = !this.showLangs;
  }

  goHome = (): any => this.router.navigateByUrl('/');
}

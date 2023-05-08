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
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { AfterViewInit, Component, inject, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { SessionStorageService } from 'ngx-webstorage';
import { Observable } from 'rxjs';
import { ConfigService } from '../../core/config/config.service';
import { SessionInfoService } from '../../core/security/session-info.service';
import { DetailsActionComponent } from '../../entities/common/details-action/details-action.component';
import { PatientActionComponent } from '../../entities/common/patient-action/patient-action.component';
import { changeLang, IHumanDTO, LANGUAGES } from '../../shared';
import { PatientComponent } from '../../shared/component/patient/patient.component';
import { SharedDataService } from '../../shared/services/shared-data.service';
import { SharedLibsModule } from '../../shared/shared-libs.module';

/**
 * Used to display the top bar.
 */
@Component({
  standalone: true,
  selector: 'vm-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  imports: [SharedLibsModule, PatientComponent, PatientActionComponent, DetailsActionComponent],
})
export class NavbarComponent implements OnInit, AfterViewInit {
  languages = LANGUAGES;
  currentLanguage!: string;
  translateService: TranslateService = inject(TranslateService);
  sessionStorageService: SessionStorageService = inject(SessionStorageService);
  sharedDataService: SharedDataService = inject(SharedDataService);
  configService: any = inject(ConfigService);
  canVisit$!: Observable<boolean>;

  user: IHumanDTO = {} as IHumanDTO;

  sessionInfoService: SessionInfoService = inject(SessionInfoService);
  showMobileMenu: boolean = false;
  showActionMenu: boolean = false;
  showTabletMenu: boolean = false;
  canEdit: boolean = false;
  isMobile: boolean = false;

  public getScreenWidth: any;

  constructor(private breakpointObserver: BreakpointObserver) {
    this.getScreenWidth = window.innerWidth;
    this.isMobile = this.breakpointObserver.isMatched(Breakpoints.Handset);
  }

  ngOnInit(): void {
    this.translateService.use(this.sessionInfoService.queryParams.lang?.toLocaleLowerCase().slice(0, 2)! || 'de');
    this.currentLanguage = this.sessionInfoService.queryParams.lang?.toLocaleLowerCase().slice(0, 2)! || 'de';
    this.user = this.sessionInfoService.author.getValue();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.canVisit$ = this.configService.canActivate;
      this.ngOnInit();
    }, 100);
  }

  changeLanguage = (languageKey: string): void => {
    this.currentLanguage = changeLang(
      languageKey,
      this.sessionInfoService,
      this.translateService,
      this.sessionStorageService,
      this.currentLanguage
    );
  };

  toggleMenu = (): boolean => {
    this.sharedDataService.showActionMenu = false;
    this.showTabletMenu = false;
    this.showMobileMenu = !this.showMobileMenu;
    return this.showMobileMenu;
  };
  toggleTabletMenu = (): boolean => (this.showTabletMenu = !this.showTabletMenu);
  toggleActionMenu = (): boolean => {
    this.showMobileMenu = false;
    this.showTabletMenu = false;
    this.sharedDataService.showActionMenu = !this.sharedDataService.showActionMenu;
    return this.sharedDataService.showActionMenu;
  };
  closeMenu = (): void => {
    this.showMobileMenu = false;
    this.showActionMenu = false;
    this.showTabletMenu = false;
    this.sharedDataService.showActionMenu = false;
  };
}

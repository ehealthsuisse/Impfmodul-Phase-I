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
import { AfterViewInit, Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatDrawer } from '@angular/material/sidenav';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { environment } from '../../../environment';
import { ConfigService } from '../../core/config/config.service';
import { SamlService } from '../../core/security/saml.service';
import { SessionInfoService } from '../../core/security/session-info.service';
import { DetailsActionComponent } from '../../entities/common/details-action/details-action.component';
import { PatientActionComponent } from '../../entities/common/patient-action/patient-action.component';
import { VaccinationRecordService } from '../../entities/vaccination-record/service/vaccination-record.service';
import { IHumanDTO, LANGUAGES } from '../../shared';
import { BreakPointSensorComponent } from '../../shared/component/break-point-sensor/break-point-sensor.component';
import { PatientComponent } from '../../shared/component/patient/patient.component';
import { PatientService } from '../../shared/component/patient/patient.service';
import { SharedDataService } from '../../shared/services/shared-data.service';
import { SpinnerService } from '../../shared/services/spinner.service';
import { SharedLibsModule } from '../../shared/shared-libs.module';
import { LanguageComponent } from './language/language.component';
import { NavigationComponent } from './navigation/navigation.component';

/**
 * Used to display the top bar.
 */
@Component({
  selector: 'vm-navbar',
  standalone: true,
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  imports: [SharedLibsModule, PatientComponent, PatientActionComponent, DetailsActionComponent, NavigationComponent, LanguageComponent],
})
export class NavbarComponent extends BreakPointSensorComponent implements OnInit, AfterViewInit, OnDestroy {
  languages = LANGUAGES;
  currentLanguage!: string;
  sharedDataService: SharedDataService = inject(SharedDataService);
  spinnerService: SpinnerService = inject(SpinnerService);
  configService: any = inject(ConfigService);
  canVisit$!: Observable<boolean>;
  user: IHumanDTO = {} as IHumanDTO;
  sessionInfoService: SessionInfoService = inject(SessionInfoService);
  @ViewChild('drawer') drawer!: MatDrawer;
  @ViewChild('drawerAction') drawerAction!: MatDrawer;
  samlService: SamlService = inject(SamlService);
  showActionSidenav: boolean = false;
  showActionSidenavSubscription!: Subscription;
  showPatientSidenav: boolean = false;
  showPatientSidenavSubscription!: Subscription;
  patient: BehaviorSubject<IHumanDTO> = new BehaviorSubject<IHumanDTO>({} as IHumanDTO);
  isAuthorized: boolean = false;
  patientService: PatientService = inject(PatientService);
  isVisible: boolean = true;
  vaccinationRecordService: VaccinationRecordService = inject(VaccinationRecordService);
  backendVersion?: string;
  frontendVersion: string = environment.VERSION;

  ngOnInit(): void {
    this.patientService.patient.subscribe({
      next: (patient: IHumanDTO) => {
        this.patient.next(patient);
      },
    });
    this.samlService.isSsoRedirectCompleted.subscribe({
      next: (isAuthorized: boolean) => (this.isAuthorized = isAuthorized),
    });
    this.showActionSidenavSubscription = this.dialogService.showActionSidenav$.subscribe({
      next: (show: boolean) => (this.showActionSidenav = show),
    });

    this.showPatientSidenavSubscription = this.dialogService.showPatientActionSidenav$.subscribe({
      next: (show: boolean) => (this.showPatientSidenav = show),
    });

    this.translateService.use(this.sessionInfoService.queryParams.lang?.toLocaleLowerCase().slice(0, 2)! || 'de');
    this.currentLanguage = this.sessionInfoService.queryParams.lang?.toLocaleLowerCase().slice(0, 2)! || 'de';
    this.sessionInfoService.author.subscribe({
      next: (user: IHumanDTO) => {
        this.user = user;
      },
    });
    this.isVisible = this.configService.isLogoutButtonVisible;
    this.initBackendVersion();
  }

  async initBackendVersion(): Promise<void> {
    await this.vaccinationRecordService.getVersion();
    this.backendVersion = this.vaccinationRecordService.backendVersion;
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.canVisit$ = this.configService.canActivate;
      this.ngOnInit();
    }, 100);
  }

  ngOnDestroy(): void {
    if (this.showActionSidenavSubscription) {
      this.showActionSidenavSubscription.unsubscribe();
    }
    if (this.showPatientSidenavSubscription) {
      this.showPatientSidenavSubscription.unsubscribe();
    }
  }
}

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Router } from '@angular/router';
import { SamlService } from '../../core/security/saml.service';
import { SpinnerService } from '../../shared/services/spinner.service';

@Component({
  selector: 'vm-welcome',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './welcome.component.html',
  styleUrls: ['./welcome.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WelcomeComponent {
  constructor(private samlService: SamlService, private router: Router, private spinnerService: SpinnerService) {
    this.cleanUpSessionStorage();
    this.cleanUpLocalStorage();
    this.samlService.authStateSubject.subscribe({
      next: authState => {
        if (authState === 'not-started') {
          this.spinnerService.show();
          this.samlService.samlLogin().subscribe({
            next: response => {
              // Manually forward in localmode (both for localhost and internal dev environments)
              if (response?.includes('localhost') || response?.includes('apps.ocp4.innershift.sodigital.io')) {
                document.location.href = response;
              } else {
                this.router.navigateByUrl('/vaccination-record');
              }
            },
            error: () => this.router.navigateByUrl('/access-denied'),
          });
        }
      },
    });
  }

  cleanUpSessionStorage = (): void => sessionStorage.removeItem('vaccination-portal-data');

  cleanUpLocalStorage = (): void => {
    localStorage.removeItem('vaccination-portal-data');
    localStorage.removeItem('validation-status');
  };
}

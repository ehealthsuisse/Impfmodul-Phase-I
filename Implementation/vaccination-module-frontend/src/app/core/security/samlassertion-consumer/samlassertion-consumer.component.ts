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
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { SamlService } from '../saml.service';
import { SessionInfoService } from '../session-info.service';

@Component({
  selector: 'vm-samlassertion-consumer',
  imports: [CommonModule],
  templateUrl: './samlassertion-consumer.component.html',
  styleUrls: ['./samlassertion-consumer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SAMLAssertionConsumerComponent implements OnInit, OnDestroy {
  subscription!: Subscription;
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private samlService: SamlService,
    private sessionInfoService: SessionInfoService
  ) {}

  ngOnInit(): void {
    this.subscription = this.route.queryParamMap.subscribe(params => {
      const samlArt = params.get('SAMLart')?.replace(/ /g, '+');
      const relayState = params.get('RelayState');

      if (!samlArt || !relayState) {
        console.error('SAMLart or RelayState not found in the URL', samlArt, relayState);
        return;
      }

      this.samlService.ssoArtifactRedirect(samlArt, relayState).subscribe({
        next: () => {
          this.sessionInfoService.initializeSessionInfo();
          this.onValidationSuccess();
          this.samlService.isSsoRedirectCompleted.next(true);
          sessionStorage.setItem('isSsoRedirectCompleted', 'true');
        },
        error: () => {
          this.samlService.isSsoRedirectCompleted.next(false);
          sessionStorage.setItem('isSsoRedirectCompleted', 'false');
        },
      });
    });
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  onValidationSuccess = (): void => {
    this.router.navigateByUrl('/vaccination-record');
  };
}

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
import { Component, inject } from '@angular/core';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { TranslateService } from '@ngx-translate/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DialogService } from '../../services';

@Component({
  selector: 'vm-break-point-sensor',
  standalone: true,
  template: ``,
  styles: [],
})
export class BreakPointSensorComponent {
  // Define your custom breakpoints
  MY_TABLET_BREAKPOINT = '(min-width: 960px) and (max-width: 1279px)';
  MY_DESKTOP_BREAKPOINT = '(min-width: 1280px)';

  isMobile: boolean = false;
  isDesktop: boolean = false;
  isTablet: boolean = false;
  translateService: TranslateService = inject(TranslateService);
  snackBar = inject(MatSnackBar);
  dialogService = inject(DialogService);

  constructor(private breakpointObserver: BreakpointObserver) {
    this.breakpointObserver.observe([Breakpoints.XSmall, Breakpoints.Small, Breakpoints.HandsetPortrait,
        Breakpoints.Web, this.MY_TABLET_BREAKPOINT, this.MY_DESKTOP_BREAKPOINT,
      ])
      .subscribe(result => {
        if (result.breakpoints[Breakpoints.XSmall] || result.breakpoints[Breakpoints.Small] ||
          result.breakpoints[Breakpoints.HandsetPortrait]
        ) {
          this.isMobile = true;
          this.isTablet = false;
          this.isDesktop = false;
        } else if (result.breakpoints[this.MY_TABLET_BREAKPOINT]) {
          this.isTablet = true;
          this.isMobile = false;
          this.isDesktop = false;
        } else if (result.breakpoints[this.MY_DESKTOP_BREAKPOINT]) {
          this.isDesktop = true;
          this.isMobile = false;
          this.isTablet = false;
        }
      });
  }
  displayMenu(showActionSidenav: boolean, showPatientActionSidenav: boolean): void {
    this.dialogService.showActionSidenav(showActionSidenav);
    this.dialogService.showPatientActionSidenav(showPatientActionSidenav);
  }
}

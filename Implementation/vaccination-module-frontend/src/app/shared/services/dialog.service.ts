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
import { inject, Injectable, Optional } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { HelpService } from '../component/help/service/help.service';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class DialogService {
  @Optional() translateService = inject(TranslateService);
  helpService = inject(HelpService);

  showActionSidenavSource = new BehaviorSubject<boolean>(false);
  showActionSidenav$ = this.showActionSidenavSource.asObservable();

  showPatientActionSidenavSource = new BehaviorSubject<boolean>(false);
  showPatientActionSidenav$ = this.showPatientActionSidenavSource.asObservable();

  openDialog<T>(
    title: string,
    body: string,
    showActions?: boolean,
    object?: T,
    buttons?: { showOk?: boolean | false; showDownload?: boolean | true }
  ): void {
    const options = {
      title: this.translateService.instant(title),
      body: this.translateService.instant(body),
      okClicked: false,
      showActions: showActions,
      object: object,
      button: buttons,
    };
    this.helpService.open(options);
  }

  closeDialog(): void {
    this.helpService.close();
  }

  showActionSidenav(show: boolean): void {
    this.showActionSidenavSource.next(show);
  }

  showPatientActionSidenav(show: boolean): void {
    this.showPatientActionSidenavSource.next(show);
  }
}

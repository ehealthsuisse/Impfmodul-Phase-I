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
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnDestroy } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { SpinnerService } from '../../services/spinner.service';
import { BaseHttpService } from '../../../shared/services/base-http.service';
import { VaccinationRecordService } from '../../../entities/vaccintion-record/service/vaccination-record.service';
import { catchError, combineLatest, map, of, Subject, timer } from 'rxjs';
import { filter, first, takeUntil } from 'rxjs/operators';

@Component({
  standalone: true,
  imports: [CommonModule],
  template: '',
  styleUrls: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageTitleTranslateComponent implements OnDestroy {
  translateService: TranslateService = inject(TranslateService);
  route: ActivatedRoute = inject(ActivatedRoute);
  pageTitle = '';
  private titleService: Title = inject(Title);
  private translation$: Subject<void> = new Subject<void>();

  constructor(
    public spinnerService: SpinnerService,
    public baseServices: BaseHttpService<any>,
    public vaccinationRService: VaccinationRecordService
  ) {
    setTimeout(() => {
      combineLatest([this.route.data, timer(0, 2000)])
        .pipe(
          map(([routeData]) => {
            const pageTitle = routeData['pageTitle'];
            const pageTitleTranslated = this.translateService.instant(pageTitle);

            return pageTitle !== pageTitleTranslated && pageTitleTranslated;
          }),
          filter(pageTitle => pageTitle),
          takeUntil(this.translation$),
          first(),
          catchError(err => of(err))
        )
        .subscribe(pageTitle => {
          this.pageTitle = pageTitle;
          this.titleService.setTitle(pageTitle);
        });
    }, 0);
  }

  ngOnDestroy(): void {
    this.translation$.next();
    this.translation$.complete();
  }
}

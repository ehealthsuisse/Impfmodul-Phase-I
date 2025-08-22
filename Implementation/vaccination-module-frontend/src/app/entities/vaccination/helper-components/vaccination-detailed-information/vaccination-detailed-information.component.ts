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
import { ChangeDetectionStrategy, Component, Input, Optional } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { IValueDTO } from 'src/app/shared';
import { IVaccination } from '../../../../model';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { ConfirmComponent } from '../../../../shared/component/confirm/confirm.component';

@Component({
  selector: 'vm-vaccination-detailed-information',
  standalone: true,
  imports: [SharedLibsModule],
  templateUrl: './vaccination-detailed-information.component.html',
  styleUrls: ['./vaccination-detailed-information.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VaccinationDetailedInformationComponent {
  @Input() vaccination: IVaccination | null = null;
  @Input() showTitle: boolean = false;
  commentsOpened: boolean = false;

  constructor(private translateService: TranslateService, @Optional() private confirmParent: ConfirmComponent) {}

  get isConfirmComponentParent(): boolean {
    return !!this.confirmParent;
  }

  toggleComments(): void {
    this.commentsOpened = !this.commentsOpened;
  }

  translateAndConcatenate(targetDiseases: IValueDTO[] | undefined): string | undefined {
    if (!!targetDiseases) {
      return targetDiseases.map(dto => this.translateService.instant('vaccination-targetdiseases.' + dto.code)).join('; ');
    }
    return undefined;
  }

  getTranslatedVaccinationReason(reasonCode?: string): string {
    return this.translateService.instant('VACCINATION_REASON.' + (reasonCode || 'DEFAULT'));
  }
}

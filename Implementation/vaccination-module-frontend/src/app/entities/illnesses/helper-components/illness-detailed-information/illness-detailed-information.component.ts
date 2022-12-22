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
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { PerformerToStringPipe } from '../../../../shared/pipes/performer-to-string.pipe';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from '@angular/flex-layout';
import { IIllnesses } from '../../../../model/illnesses.interface';

@Component({
  selector: 'vm-illness-detailed-information',
  standalone: true,
  imports: [CommonModule, SharedLibsModule, PerformerToStringPipe, TranslateModule, FlexLayoutModule],
  templateUrl: './illness-detailed-information.component.html',
  styleUrls: ['./illness-detailed-information.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IllnessDetailedInformationComponent {
  @Input() illness: IIllnesses | null = null;
}

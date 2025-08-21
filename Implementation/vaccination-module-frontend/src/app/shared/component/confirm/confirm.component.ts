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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { SharedLibsModule } from '../../shared-libs.module';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Router } from '@angular/router';

@Component({
  selector: 'vm-confirm',
  standalone: true,
  imports: [SharedLibsModule],
  templateUrl: './confirm.component.html',
  styleUrls: ['./confirm.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmComponent {
  @Input() data: any;
  isMobile: boolean = false;
  isDetailPage: boolean = false;
  constructor(
    public dialogRef: MatDialogRef<ConfirmComponent>,
    private breakpointObserver: BreakpointObserver,
    private route: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.isDetailPage = this.route.url.includes('detail');

    this.breakpointObserver.observe(['(max-width: 715px)']).subscribe(result => {
      this.isMobile = result.matches;
      this.cdr.markForCheck();
    });

    dialogRef.backdropClick().subscribe(() => {
      this.dialogRef.close();
    });
  }

  onSaveAndStay(): void {
    this.dialogRef.close({ action: 'SAVE_AND_STAY' });
  }

  save(): void {
    this.dialogRef.close({ action: 'SAVE' });
  }

  delete(): void {
    this.dialogRef.close({ action: 'DELETE' });
  }
}

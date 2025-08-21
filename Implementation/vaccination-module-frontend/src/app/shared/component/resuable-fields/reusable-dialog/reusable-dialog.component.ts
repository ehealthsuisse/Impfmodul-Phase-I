/**
 * Copyright (c) 2025 eHealth Suisse
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
import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogRef } from '@angular/material/dialog';
import { BreakpointObserver } from '@angular/cdk/layout';
import { NgClass, NgIf } from '@angular/common';
import { TranslateDirective } from '../../../language';
import { TranslateModule } from '@ngx-translate/core';
import { MatButton } from '@angular/material/button';
import { MatCardContent } from '@angular/material/card';

/**
 * Reusable confirm dialog with Yes and No buttons, used when deleting a record or converting a record from Immunization
 * Administration to Vaccination Record
 */
@Component({
  selector: 'vm-reusable-dialog',
  standalone: true,
  imports: [
    MatButton,
    MatDialogContent,
    MatCardContent,
    MatDialogActions,
    NgIf,
    TranslateModule,
    MatDialogClose,
    TranslateDirective,
    NgClass,
    MatButton,
    MatCardContent,
    MatDialogActions,
    MatDialogContent,
  ],
  templateUrl: './reusable-dialog.component.html',
  styleUrl: './reusable-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReusableDialogComponent {
  isMobile: boolean = false;
  isDesktop: boolean = false;
  constructor(
    public dialogRef: MatDialogRef<ReusableDialogComponent>,
    private breakpointObserver: BreakpointObserver,
    @Inject(MAT_DIALOG_DATA) public data: { message: string }
  ) {
    this.breakpointObserver.observe(['(max-width: 600px)']).subscribe({
      next: result => {
        this.isMobile = result.matches;
      },
    });

    this.breakpointObserver.observe(['(min-width: 601px)']).subscribe({
      next: result => {
        this.isDesktop = result.matches;
      },
    });
    dialogRef.backdropClick().subscribe(() => {
      this.dialogRef.close();
    });
  }

  onNoClick(): void {
    this.dialogRef.close(false);
  }

  onYesClick(): void {
    this.dialogRef.close(true);
  }
}

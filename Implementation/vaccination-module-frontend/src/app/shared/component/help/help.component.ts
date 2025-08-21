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
import { Component, HostListener, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SharedLibsModule } from '../../shared-libs.module';
import { DialogService } from '../../services';

/**
 * Component used to display the help content.
 */
@Component({
  selector: 'vm-help',
  standalone: true,
  imports: [SharedLibsModule],
  templateUrl: './help.component.html',
  styleUrls: ['./help.component.scss'],
})
export class HelpComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: {
      cancelText: string;
      confirmText: string;
      body: string;
      title: string;
      okClicked: boolean;
      downloadClicked: boolean;
      showActions: boolean;
      showOk: boolean;
      showDownload: boolean;
      object: any;
      button: { showOk?: boolean; showDownload?: boolean; showCancel?: boolean };
    },
    private mdDialogRef: MatDialogRef<HelpComponent>,
    private dialogService: DialogService
  ) {}

  public cancel(): void {
    this.close(false);
  }

  public close(value: any): void {
    this.mdDialogRef.close(value);
    this.dialogService.closeDialog();
  }

  public closeWithX(): void {
    this.mdDialogRef.close();
  }

  public confirm(): void {
    this.close(true);
  }

  // Neue Methode 'ok'
  public ok(): void {
    this.close(true);
  }

  public download(): void {
    const pdfContent = this.data.object; // Byte[] containing the PDF content

    const blob = new Blob([pdfContent], { type: 'application/pdf' });

    const downloadLink = document.createElement('a');
    downloadLink.href = URL.createObjectURL(blob);
    downloadLink.download = `${this.data.object}`; // Set the desired filename with the .pdf extension
    downloadLink.click();
    this.close(true);
  }

  @HostListener('keydown.esc')
  public onEsc(): void {
    this.close(false);
  }
}

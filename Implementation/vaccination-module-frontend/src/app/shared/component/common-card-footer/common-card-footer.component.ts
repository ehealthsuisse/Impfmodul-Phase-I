﻿/**
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
import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { SharedLibsModule } from '../../shared-libs.module';
import { GenericButtonComponent } from '../generic-button/generic-button.component';
import { HelpButtonComponent } from '../help-button/help-button.component';
import { DialogService } from '../../services/dialog.service';
import { IBaseDTO } from '../../interfaces/baseDTO.interface';

/**
 * Used to structure all buttons in the details and edit components.
 */
@Component({
  selector: 'vm-common-card-footer',
  standalone: true,
  imports: [SharedLibsModule, GenericButtonComponent, HelpButtonComponent],
  templateUrl: './common-card-footer.component.html',
  styleUrls: ['./common-card-footer.component.scss'],
})
export class CommonCardFooterComponent<T extends IBaseDTO> {
  @Output() edit: EventEmitter<T> = new EventEmitter<T>();
  @Output() delete: EventEmitter<T> = new EventEmitter<T>();
  @Output() help: EventEmitter<Event> = new EventEmitter<Event>();
  @Output() save: EventEmitter<Event> = new EventEmitter<Event>();
  @Output() validate: EventEmitter<Event> = new EventEmitter<Event>();
  @Output() download: EventEmitter<Event> = new EventEmitter<Event>();

  @Input() validationEnables: boolean = false;

  @Input() item: T | null = null;
  @Input() dialogTitle!: string;
  @Input() dialogBody!: string;
  @Input() isDetailsFooter!: boolean;
  @Input() isEditFooter!: boolean;
  @Input() canValidate!: boolean;
  @Input() disabled!: boolean;
  @Input() IsNewRecord!: boolean;

  dialog = inject(DialogService);

  navigateToEdit = (value: T): void => this.edit.emit(value);
  saveRecord = (event: Event): void => this.save.emit(event);
  deleteRecord = (value: T): void => this.delete.emit(value);

  previous = (): void => window.history.back();

  openDialog(event: Event): void {
    this.dialog.openDialog(this.dialogTitle, this.dialogBody);
    this.help.emit(event);
  }
  validateRecord = (event: Event): void => this.validate.emit(event);

  downloadRecord = (event: Event): void => this.download.emit(event);
}

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
import { Component, EventEmitter, inject, Input, OnInit, Output } from '@angular/core';
import { DialogService } from '../../services';
import { SharedLibsModule } from '../../shared-libs.module';
import { BreakPointSensorComponent } from '../break-point-sensor/break-point-sensor.component';
import { SessionInfoService } from '../../../core/security/session-info.service';

/**
 * Used for the banner on top of the tables containig the title, searchbar, add- and help-button.
 */
@Component({
  selector: 'vm-list-header',
  standalone: true,
  imports: [SharedLibsModule],
  templateUrl: './list-header.component.html',
  styleUrls: ['./list-header.component.scss'],
})
export class ListHeaderComponent<T> extends BreakPointSensorComponent implements OnInit {
  @Input() showPatientName: boolean = false;
  @Input() subtitleVisibility: boolean = true;

  @Output() filter: EventEmitter<Event> = new EventEmitter<Event>();
  @Output() add: EventEmitter<T> = new EventEmitter<T>();
  @Input() isVisible: boolean = true;
  @Input() item: T | null = null;
  @Input() helpTitle: string = '';
  @Input() helpBody: string = '';
  @Input() title: string = 'VACCINATION.TITLE';

  @Output() help: EventEmitter<Event> = new EventEmitter<Event>();
  @Input() toggle: boolean = true;

  isEmergencyMode: boolean = false;
  sessionInfoService: SessionInfoService = inject(SessionInfoService);
  dialog: DialogService = inject(DialogService);

  filterValue = (event: Event): void => {
    this.filter.emit(event);
  };

  openHelpDialog(event: Event): void {
    this.dialog.openDialog(this.helpTitle, this.helpBody);
    this.help.emit(event);
  }

  ngOnInit(): void {
    this.isEmergencyMode = this.sessionInfoService.isEmergencyMode();
  }
}

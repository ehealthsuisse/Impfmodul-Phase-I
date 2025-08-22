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
import { ChangeDetectionStrategy, Component, EventEmitter, inject, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FlexModule } from '@angular/flex-layout';
import { RouterLinkActive, RouterLinkWithHref } from '@angular/router';
import { TranslateDirective } from '../../../shared';
import { SharedDataService } from '../../../shared/services/shared-data.service';
import { BreakPointSensorComponent } from '../../../shared/component/break-point-sensor/break-point-sensor.component';
import { MatIconModule } from '@angular/material/icon';
import { SharedModule } from '../../../shared/shared.module';

@Component({
  selector: 'vm-navigation',
  standalone: true,
  imports: [CommonModule, FlexModule, RouterLinkActive, RouterLinkWithHref, TranslateDirective, MatIconModule, SharedModule],
  templateUrl: './navigation.component.html',
  styleUrls: ['./navigation.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavigationComponent extends BreakPointSensorComponent implements OnInit {
  showTabletMenu: boolean = false;
  sharedDataService: SharedDataService = inject(SharedDataService);
  @Output() toggleSidenav = new EventEmitter<void>();

  ngOnInit(): void {
    this.showTabletMenu = false;
  }
  closeSidenav(): void {
    this.toggleSidenav.emit();
  }
}

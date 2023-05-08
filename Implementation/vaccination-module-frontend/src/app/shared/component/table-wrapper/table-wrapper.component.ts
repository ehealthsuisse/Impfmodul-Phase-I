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
import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  ViewChild,
} from '@angular/core';
import { MatSort } from '@angular/material/sort';
import { MatTable, MatTableDataSource } from '@angular/material/table';
import { TranslateModule } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { parseStringToDate } from '../../function';
import { TranslateDirective } from '../../language';
import { MaterialModule } from '../../material.module';
import { ConvertToSnakeCaseTranslationPipe } from '../../pipes';
import { SharedDataService } from '../../services/shared-data.service';
import { BreakPointSensorComponent } from '../break-point-sensor/break-point-sensor.component';
import { ListHeaderComponent } from '../list-header';
import { RefreshIndicatorComponent } from '../refresh-indicator';

@Component({
  selector: 'vm-table-wrapper',
  standalone: true,
  imports: [
    CommonModule,
    MaterialModule,
    RefreshIndicatorComponent,
    TranslateModule,
    TranslateDirective,
    ConvertToSnakeCaseTranslationPipe,
    ListHeaderComponent,
  ],
  templateUrl: './table-wrapper.component.html',
  styleUrls: ['./table-wrapper.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TableWrapperComponent<T> extends BreakPointSensorComponent implements OnDestroy, OnChanges, AfterViewInit {
  @Input() displayedColumns!: string[];
  @Input() translationPrefix!: string;
  @Input() columnsPrefix: string = '';
  @Input() dataLoading = false;
  @Input() dataSource!: MatTableDataSource<T>;
  @Input() fullWidth = '80vw';

  @Input() tableName = '';

  @Output() rowClick: EventEmitter<T> = new EventEmitter<T>();

  @ViewChild('table') table!: MatTable<T>;

  sortables: string[] = ['recordedDate', 'occurrenceDate', 'doseNumber', 'recorder', 'vaccineCode'];
  canValidated!: boolean;

  sharedDataService: SharedDataService = inject(SharedDataService);
  unsubscribe: Subject<void> = new Subject<void>();

  @ViewChild(MatSort) sort!: MatSort;

  ngAfterViewInit(): void {
    this.canValidated = this.sharedDataService.storedData['role'] === 'HCP' || this.sharedDataService.storedData['role'] === 'ASS';
    if (this.dataSource) {
      this.dataSource.sort = this.sort;
    }
  }
  ngOnChanges(): void {
    if (this.dataSource) {
      this.dataSource.sort = this.sort;
      this.dataSource.sortingDataAccessor = sortingDataAccessorCustom;
    }
  }
  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  isSortable = (col: string): boolean => {
    return this.sortables.includes(col);
  };
}

const sortingDataAccessorCustom = <T>(data: T, sortHeaderId: string): string | number => {
  const val = data[sortHeaderId as keyof T];
  const parsedAsDate = parseStringToDate(`${val}`);

  if (parsedAsDate) {
    return parsedAsDate.getTime();
  }
  if (!isNaN(+val)) {
    return +val;
  }
  return `${val}`;
};

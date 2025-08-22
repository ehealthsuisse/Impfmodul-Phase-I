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
import { MaterialModule } from '../../material.module';
import { ConvertToSnakeCaseTranslationPipe } from '../../pipes';
import { SharedDataService } from '../../services/shared-data.service';
import { BreakPointSensorComponent } from '../break-point-sensor/break-point-sensor.component';
import { RefreshIndicatorComponent } from '../refresh-indicator';
import { type GroupedRecord } from '../../interfaces/groupedRecord.interface';

@Component({
  selector: 'vm-table-wrapper',
  standalone: true,
  imports: [CommonModule, MaterialModule, RefreshIndicatorComponent, TranslateModule, ConvertToSnakeCaseTranslationPipe],
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
  @Input() enableGrouping = true;
  @Output() rowClick: EventEmitter<T> = new EventEmitter<T>();

  @ViewChild('table') table!: MatTable<T>;
  @ViewChild(MatSort) sort!: MatSort;

  sortables: string[] = ['recordedDate', 'occurrenceDate', 'doseNumber', 'recorder', 'vaccineCode'];
  canValidated!: boolean;
  sharedDataService: SharedDataService = inject(SharedDataService);
  unsubscribe: Subject<void> = new Subject<void>();

  groupedDataSource!: MatTableDataSource<GroupedRecord<T>>;
  private originalGroupedData!: GroupedRecord<T>[];

  ngAfterViewInit(): void {
    this.canValidated = this.sharedDataService.storedData['role'] === 'HCP' || this.sharedDataService.storedData['role'] === 'ASS';
    if (this.dataSource) {
      this.setupDataSource();
    }
  }

  ngOnChanges(): void {
    if (this.dataSource) {
      this.setupDataSource();
    }
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  isSortable = (col: string): boolean => this.sortables.includes(col);

  onRowClick(element: GroupedRecord<T>): void {
    if (element.record) {
      this.rowClick.emit(element.record);
    }
  }

  toggleExpand(element: GroupedRecord<T>): void {
    if (element.isParent) {
      element.isExpanded = !element.isExpanded;
      this.refreshDataSource();
    }
  }

  hasRelatedRecords(): boolean {
    return this.dataSource?.data.some((record: any) => record.relatedId !== null && record.relatedId !== undefined);
  }

  get columnsToDisplay(): string[] {
    return this.enableGrouping && this.hasRelatedRecords() ? ['expand', ...this.displayedColumns] : this.displayedColumns;
  }

  get dataSourceForTemplate(): MatTableDataSource<GroupedRecord<T>> {
    return this.groupedDataSource;
  }

  private buildChainFromRoot(root: T, allRecords: T[]): T[] {
    const chain = [root];
    let current = root;

    while (true) {
      const nextRecord = allRecords.find(r => (r as any).relatedId === (current as any).id);
      if (nextRecord) {
        chain.push(nextRecord);
        current = nextRecord;
      } else {
        break;
      }
    }
    return chain;
  }

  private buildCompleteChain(startRecord: T, allRecords: T[], processedRecords: Set<any>): T[] {
    const root = this.findChainRoot(startRecord, allRecords);
    const chain = this.buildChainFromRoot(root, allRecords);
    chain.forEach(record => processedRecords.add((record as any).id));
    return chain;
  }

  private configureTableSearch(): void {
    // Set up custom filter predicate for grouped data
    this.groupedDataSource.filterPredicate = (data: GroupedRecord<T>, filter: string): boolean => {
      return this.isRecordMatchingSearch(data.record, filter);
    };

    // Watch for changes in the original dataSource filter and apply them to grouped data
    Object.defineProperty(this.dataSource, 'filter', {
      get: () => this.groupedDataSource.filter,
      set: (value: string) => {
        // Reset to original grouped data before applying filter
        this.groupedDataSource.data = this.originalGroupedData;
        this.groupedDataSource.filter = value;
        // Then refresh to handle expanded state
        this.refreshDataSource(); //
      },
    });
  }

  private createGroupedStructure(records: T[]): GroupedRecord<T>[] {
    if (!records?.length) {
      return [];
    }

    const recordMap = new Map<any, T>();
    records.forEach(record => recordMap.set((record as any).id, record));

    const rootRecords = records.filter(record => {
      const relatedId = (record as any).relatedId;
      return !relatedId || !recordMap.has(relatedId);
    });

    const chains: T[][] = [];
    const processedRecords = new Set<any>();

    [...rootRecords, ...records].forEach(record => {
      if (!processedRecords.has((record as any).id)) {
        const chain = this.buildCompleteChain(record, records, processedRecords);
        if (chain.length > 0) {
          chains.push(chain);
        }
      }
    });

    return chains.map(chain =>
      chain.length === 1
        ? { record: chain[0], isParent: false, isExpanded: false, children: [], level: 0 }
        : {
            record: chain[chain.length - 1],
            isParent: true,
            isExpanded: false,
            children: chain.slice(0, -1).reverse(),
            level: 0,
          }
    );
  }

  private findChainRoot(record: T, allRecords: T[]): T {
    let current = record;
    const visited = new Set<any>();

    while ((current as any).relatedId && !visited.has((current as any).id)) {
      visited.add((current as any).id);
      const parent = allRecords.find(r => (r as any).id === (current as any).relatedId);
      if (parent) {
        current = parent;
      } else {
        break;
      }
    }
    return current;
  }

  private isRecordMatchingSearch(record: T, searchTerm: string): boolean {
    if (!searchTerm) {
      return true;
    }

    // Convert record to searchable string (similar to MatTableDataSource default behavior)
    const searchableText = Object.values(record as Record<string, unknown>)
      .reduce((accumulator: string, propertyValue: unknown) => {
        const valueAsString = propertyValue ? propertyValue.toString().toLowerCase() : '';
        return accumulator + valueAsString + '◬'; // ◬ is used as separator to avoid false matches
      }, '')
      .toLowerCase();

    // Check if the search term exists anywhere in the searchable text
    return searchableText.indexOf(searchTerm) !== -1;
  }

  private refreshDataSource(): void {
    const baseData = this.originalGroupedData;
    const expandedData: GroupedRecord<T>[] = [];

    baseData.forEach(item => {
      expandedData.push(item);
      if (item.isParent && item.isExpanded && item.children) {
        item.children.forEach(child => {
          expandedData.push({
            record: child,
            isParent: false,
            isExpanded: false,
            children: [],
            level: 1,
          });
        });
      }
    });

    const currentFilter = this.groupedDataSource.filter;
    this.groupedDataSource.data = expandedData;
    if (currentFilter) {
      this.groupedDataSource.filter = currentFilter;
    }
  }

  private setupDataSource(): void {
    const shouldGroup = this.enableGrouping && this.hasRelatedRecords();

    this.originalGroupedData = shouldGroup
      ? this.createGroupedStructure(this.dataSource.data)
      : this.dataSource.data.map(
          record =>
            ({
              record,
              isParent: false,
              isExpanded: false,
              children: [],
              level: 0,
            } as GroupedRecord<T>)
        );

    this.groupedDataSource = new MatTableDataSource(this.originalGroupedData);

    this.configureTableSearch();
    this.refreshDataSource();

    if (this.sort) {
      this.groupedDataSource.sort = this.sort;
      this.groupedDataSource.sortingDataAccessor = (item: GroupedRecord<T>, property: string) => {
        const val = item.record[property as keyof T];
        const parsedAsDate = parseStringToDate(`${val}`);
        if (parsedAsDate) {
          return parsedAsDate.getTime();
        }
        if (!isNaN(+val)) {
          return +val;
        }
        return `${val}`;
      };
    }
  }
}

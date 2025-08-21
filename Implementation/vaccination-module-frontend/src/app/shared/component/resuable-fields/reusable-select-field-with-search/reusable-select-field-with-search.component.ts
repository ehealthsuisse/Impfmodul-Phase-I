import { Component, ElementRef, inject, Input, OnChanges, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { FormControl, NG_VALUE_ACCESSOR, SelectControlValueAccessor } from '@angular/forms';
import { MatOption } from '@angular/material/core';
import { combineLatest, map, Observable, of, ReplaySubject, startWith, Subject, switchMap } from 'rxjs';

import { IValueDTO } from '../../../interfaces';
import { SharedLibsModule } from '../../../shared-libs.module';
import { FilterPipePipe } from '../../../pipes/filter-pipe.pipe';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { TranslateService } from '@ngx-translate/core';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'vm-reusable-select-field-with-search',
  standalone: true,
  imports: [SharedLibsModule, FilterPipePipe],
  templateUrl: './reusable-select-field-with-search.component.html',
  styleUrls: ['./reusable-select-field-with-search.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: ReusableSelectFieldWithSearchComponent,
      multi: true,
    },
  ],
})
export class ReusableSelectFieldWithSearchComponent extends SelectControlValueAccessor implements OnInit, OnChanges, OnDestroy {
  @Input() formControl: FormControl = new FormControl();
  @Input() filteredList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  @Input() options$: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  @Input() translatedValue: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  @Input() isEditable!: boolean;
  @Input() item!: IValueDTO;
  @Input() translationKey!: string;
  @Input() labelKey!: string;
  @Input() isRequired: boolean = false;
  @ViewChildren(MatOption, { read: ElementRef }) matOptions!: QueryList<ElementRef>;
  @ViewChild('searchComponent') searchComponent!: any;
  searchControl = new FormControl();

  breakPoint: BreakpointObserver = inject(BreakpointObserver);
  isMobile: boolean = false;
  isDesktop: boolean = false;

  translateService: TranslateService = inject(TranslateService);
  sortedOptions$: Observable<any> | undefined;
  private destroy$: Subject<void> = new Subject<void>();
  private languageChange$: Subject<void> = new Subject<void>();

  ngOnInit(): void {
    this.breakPoint.observe(Breakpoints.Handset).subscribe(result => {
      this.isMobile = result.matches;
    });

    this.breakPoint.observe(Breakpoints.Tablet).subscribe(result => {
      this.isDesktop = result.matches;
    });

    this.languageChange$.next();
    this.initializeSortedOptions();
  }

  ngOnChanges(): void {
    this.translateService.onLangChange.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.languageChange$.next();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  override writeValue(value: any): void {
    super.writeValue(value);
  }

  compareFn(c1: IValueDTO, c2: IValueDTO): boolean {
    return c1 && c2 ? c1.code === c2.code : c1 === c2;
  }

  // logic for sorting drop-down values (vaccines, adverse effects, infectious diseases and risk factors) alphabetically
  private initializeSortedOptions(): void {
    this.sortedOptions$ = combineLatest([this.options$, this.languageChange$.pipe(startWith(null))]).pipe(
      map(([options]) => options || []),
      switchMap(options => {
        if (!options.length) {
          return of(options);
        }

        const translationKeys = options.map(option => `${this.labelKey}.${option.code}`);
        return this.translateService.get(translationKeys).pipe(
          map((translations: Record<string, string>) => {
            // Separate "unknown" option
            const unknownOption = options.find(option => option.code === '787859002');
            const otherOptions = options.filter(option => option.code !== '787859002');

            // Sort other options alphabetically based on their translations
            const sortedOptions = otherOptions.sort((a, b) => {
              const valueA = translations[`${this.labelKey}.${a.code}`]?.trim() || '';
              const valueB = translations[`${this.labelKey}.${b.code}`]?.trim() || '';
              return valueA.localeCompare(valueB);
            });

            // Add "unknown" option at the end, if it exists
            return unknownOption ? [...sortedOptions, unknownOption] : sortedOptions;
          })
        );
      })
    );
  }
}

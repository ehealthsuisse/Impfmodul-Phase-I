import { Component, ElementRef, inject, Input, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { FormControl, NG_VALUE_ACCESSOR, SelectControlValueAccessor } from '@angular/forms';
import { MatOption } from '@angular/material/core';
import { ReplaySubject } from 'rxjs';

import { IValueDTO } from '../../../interfaces';
import { SharedLibsModule } from '../../../shared-libs.module';
import { FilterPipePipe } from '../../../pipes/filter-pipe.pipe';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';

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
export class ReusableSelectFieldWithSearchComponent extends SelectControlValueAccessor implements OnInit {
  @Input() formControl: FormControl = new FormControl();
  @Input() filteredList: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  @Input() options$: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  @Input() translatedValue: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  @Input() isEditable!: boolean;
  @Input() item!: IValueDTO;
  @Input() translationKey!: string;
  @Input() labelKey!: string;
  @ViewChildren(MatOption, { read: ElementRef }) matOptions!: QueryList<ElementRef>;
  @ViewChild('searchComponent') searchComponent!: any;
  searchControl = new FormControl();

  breakPoint: BreakpointObserver = inject(BreakpointObserver);
  isMobile: boolean = false;
  isDesktop: boolean = false;

  ngOnInit(): void {
    this.breakPoint.observe(Breakpoints.Handset).subscribe(result => {
      this.isMobile = result.matches;
    });

    this.breakPoint.observe(Breakpoints.Tablet).subscribe(result => {
      this.isDesktop = result.matches;
    });
  }

  override writeValue(value: any): void {
    super.writeValue(value);
  }
  compareFn(c1: IValueDTO, c2: IValueDTO): boolean {
    return c1 && c2 ? c1.code === c2.code : c1 === c2;
  }
}

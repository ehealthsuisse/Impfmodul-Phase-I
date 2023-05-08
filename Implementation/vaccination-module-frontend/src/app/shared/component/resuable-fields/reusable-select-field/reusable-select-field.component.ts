import { Component, forwardRef, inject, Input, OnInit } from '@angular/core';
import { FormControl, NG_VALUE_ACCESSOR, SelectControlValueAccessor } from '@angular/forms';

import { Observable } from 'rxjs';
import { SharedLibsModule } from '../../../shared-libs.module';
import { IValueDTO } from '../../../interfaces';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';

@Component({
  selector: 'vm-reusable-select-field',
  standalone: true,
  imports: [SharedLibsModule],
  templateUrl: './reusable-select-field.component.html',
  styleUrls: ['./reusable-select-field.component.scss'],
  providers: [
    {
      multi: true,
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ReusableSelectFieldComponent),
    },
  ],
})
export class ReusableSelectFieldComponent extends SelectControlValueAccessor implements OnInit {
  @Input() formControl!: FormControl;
  @Input() isEditable!: boolean;
  @Input() placeholder!: string;
  @Input() options$!: Observable<any>;
  @Input() translationKey!: string;
  @Input() labelTranslationKey!: string;
  @Input() mainValueKey!: string;

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

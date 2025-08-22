import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { ControlValueAccessor, FormControl, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatDatepicker } from '@angular/material/datepicker';
import { SharedLibsModule } from '../../../shared-libs.module';
import { BreakPointSensorComponent } from '../../break-point-sensor/break-point-sensor.component';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE } from '@angular/material/core';
import { MomentDateAdapter } from '@angular/material-moment-adapter';
import { DATE_FORMAT } from '../../../date';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'vm-reusable-date-field',
  standalone: true,
  imports: [SharedLibsModule],
  templateUrl: './reusable-date-field.component.html',
  styleUrls: ['./reusable-date-field.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ReusableDateFieldComponent),
      multi: true,
    },
    {
      provide: DateAdapter,
      useClass: MomentDateAdapter,
      deps: [MAT_DATE_LOCALE],
    },
    { provide: MAT_DATE_FORMATS, useValue: DATE_FORMAT },
  ],
})
export class ReusableDateFieldComponent
  extends BreakPointSensorComponent
  implements OnDestroy, AfterViewInit, OnChanges, ControlValueAccessor
{
  @Input() formControl!: FormControl;
  @Input() isEditable!: boolean;
  @ViewChild('picker') picker!: MatDatepicker<Date>;

  @Input() formGroup!: FormGroup;

  @Input() translationKey!: string;
  @Input() labelTranslationKey!: string;
  @Input() mainValueKey!: string;
  @Input() notEditableValue: string = '';

  visibleError: string | null = null;
  private destroy$: Subject<void> = new Subject<void>();

  ngAfterViewInit(): void {
    if (this.formGroup) {
      this.formGroup
        .get('begin')
        ?.valueChanges.pipe(takeUntil(this.destroy$))
        .subscribe(() => {
          this.formGroup.get('end')?.updateValueAndValidity();
          this.formGroup.get('recordedDate')?.updateValueAndValidity();
        });
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['formControl']) {
      this.formControl?.statusChanges.subscribe(() => this.updateVisibleError());
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onChange: any = () => {};
  onTouched: any = () => {};

  writeValue(obj: any): void {
    this.onChange(obj);
  }
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  // Updates the `visibleError` property to display the first validation error from the form control.
  // Handles cases where multiple errors occur simultaneously, such as when the begin date is in the future
  // and the date of diagnosis is both in the future and before the begin date.
  private updateVisibleError(): void {
    this.visibleError = this.formControl?.invalid
      ? ['endDateBeforeStartDate', 'futureDate', 'startDateBeforeEndDate'].find(error => this.formControl.hasError(error)) || null
      : null;
  }
}

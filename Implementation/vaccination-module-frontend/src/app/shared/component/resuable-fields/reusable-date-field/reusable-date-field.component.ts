import { AfterViewInit, ChangeDetectionStrategy, Component, forwardRef, Input, OnDestroy, ViewChild } from '@angular/core';
import { FormControl, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
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
export class ReusableDateFieldComponent extends BreakPointSensorComponent implements OnDestroy, AfterViewInit {
  @Input() formControl!: FormControl;
  @Input() isEditable!: boolean;
  @ViewChild('picker') picker!: MatDatepicker<Date>;

  @Input() formGroup!: FormGroup;

  @Input() translationKey!: string;
  @Input() labelTranslationKey!: string;
  @Input() mainValueKey!: string;
  @Input() notEditableValue: string = '';

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
}

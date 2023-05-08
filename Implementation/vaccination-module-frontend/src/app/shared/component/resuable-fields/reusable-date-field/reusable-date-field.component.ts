import { ChangeDetectionStrategy, Component, forwardRef, Input, ViewChild } from '@angular/core';
import { FormControl, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatDatepicker } from '@angular/material/datepicker';
import { SharedLibsModule } from '../../../shared-libs.module';
import { BreakPointSensorComponent } from '../../break-point-sensor/break-point-sensor.component';

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
  ],
})
export class ReusableDateFieldComponent extends BreakPointSensorComponent {
  @Input() formControl!: FormControl;
  @Input() isEditable!: boolean;
  @ViewChild('picker') picker!: MatDatepicker<Date>;

  @Input() formGroup!: FormGroup;

  @Input() translationKey!: string;
  @Input() labelTranslationKey!: string;

  @Input() mainValueKey!: string;

  @Input() notEditableValue: string = '';

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

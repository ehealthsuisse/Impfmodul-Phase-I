import { AfterViewInit, Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { SharedLibsModule } from '../../../../shared-libs.module';
import { IHumanDTO } from '../../../../interfaces';
import { BreakPointSensorComponent } from '../../../break-point-sensor/break-point-sensor.component';

@Component({
  selector: 'vm-reusable-form-recorder-field',
  standalone: true,
  imports: [SharedLibsModule],
  templateUrl: './reusable-form-recorder-field.component.html',
  styleUrls: ['./reusable-form-recorder-field.component.scss'],
})
export class ReusableFormRecorderFieldComponent extends BreakPointSensorComponent implements AfterViewInit {
  @Input() parentForm!: FormGroup;
  @Input() isEditable!: boolean;
  @Input() recorder!: IHumanDTO;

  ngAfterViewInit(): void {
    this.parentForm.controls['recorder'].get('firstName')?.markAsTouched();
    this.parentForm.controls['recorder'].get('lastName')?.markAsTouched();
  }

  trimControl(path: string): void {
    const control = this.parentForm.get(path);
    const value = control?.value;
    if (typeof value === 'string') {
      control?.setValue(value.trim());
      this.updateRecorderValidation();
    }
  }

  isFirstNameRequired(): boolean {
    return !this.hasText('organization') || this.hasText('recorder.lastName');
  }

  isLastNameRequired(): boolean {
    return !this.hasText('organization') || this.hasText('recorder.firstName');
  }

  isOrganizationRequired(): boolean {
    return !this.hasText('recorder.firstName') || !this.hasText('recorder.lastName');
  }

  private hasText(path: string): boolean {
    const value = this.parentForm.get(path)?.value;
    return typeof value === 'string' && value.trim().length > 0;
  }

  private updateRecorderValidation(): void {
    this.parentForm.get('recorder.firstName')?.updateValueAndValidity();
    this.parentForm.get('recorder.lastName')?.updateValueAndValidity();
    this.parentForm.get('organization')?.updateValueAndValidity();
  }
}

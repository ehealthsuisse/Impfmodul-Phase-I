import { Component, Input } from '@angular/core';
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
export class ReusableFormRecorderFieldComponent extends BreakPointSensorComponent {
  @Input() parentForm!: FormGroup;
  @Input() isEditable!: boolean;
  @Input() recorder!: IHumanDTO;
}

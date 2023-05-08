import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ReusableFormRecorderFieldComponent } from './reusable-form-recorder-field/reusable-form-recorder-field.component';
import { ReusableViewRecorderFieldComponent } from './reusable-view-recorder-field/reusable-view-recorder-field.component';
import { SharedLibsModule } from '../../../shared-libs.module';
import { IHumanDTO } from '../../../interfaces';
import { BreakPointSensorComponent } from '../../break-point-sensor/break-point-sensor.component';

@Component({
  selector: 'vm-reusable-recorder-field',
  standalone: true,
  imports: [SharedLibsModule, ReusableFormRecorderFieldComponent, ReusableViewRecorderFieldComponent],
  templateUrl: './reusable-recorder-field.component.html',
  styleUrls: ['./reusable-recorder-field.component.scss'],
})
export class ReusableRecorderFieldComponent extends BreakPointSensorComponent {
  @Input() parentForm!: FormGroup;
  @Input() isEditable!: boolean;
  @Input() recorder!: IHumanDTO;
  @Input() organization!: string;
}

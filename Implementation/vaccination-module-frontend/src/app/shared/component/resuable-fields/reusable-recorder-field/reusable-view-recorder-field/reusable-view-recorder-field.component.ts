import { Component, Input } from '@angular/core';
import { SharedLibsModule } from '../../../../shared-libs.module';
import { IHumanDTO } from '../../../../interfaces';
@Component({
  selector: 'vm-reusable-view-recorder-field',
  standalone: true,
  imports: [SharedLibsModule],
  templateUrl: './reusable-view-recorder-field.component.html',
  styleUrls: ['./reusable-view-recorder-field.component.scss'],
})
export class ReusableViewRecorderFieldComponent {
  @Input() recorder!: IHumanDTO;
  @Input() organization!: string;
}

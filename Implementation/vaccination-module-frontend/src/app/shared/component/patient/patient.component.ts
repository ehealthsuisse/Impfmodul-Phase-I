/**
 * Copyright (c) 2023 eHealth Suisse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the “Software”), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, inject } from '@angular/core';
import { SessionInfoService } from 'src/app/core/security/session-info.service';
import { BreakPointSensorComponent } from '../break-point-sensor/break-point-sensor.component';
import { PatientService } from './patient.service';

@Component({
  selector: 'vm-patient',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './patient.component.html',
  styleUrls: ['./patient.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PatientComponent extends BreakPointSensorComponent implements AfterViewInit {
  patientService: PatientService = inject(PatientService);
  sessionInfoService: SessionInfoService = inject(SessionInfoService);
  cdr: ChangeDetectorRef = inject(ChangeDetectorRef);
  patient?: string;

  ngAfterViewInit(): void {
    this.patient = this.sessionInfoService.patientName;
    if (!this.patient) {
      this.patientService.fetchPatientName().subscribe({
        next: patientName => {
          this.sessionInfoService.patientName = patientName;
          this.patient = patientName;
          this.cdr.detectChanges();
        },
      });
    }
    this.cdr.detectChanges();
  }
}

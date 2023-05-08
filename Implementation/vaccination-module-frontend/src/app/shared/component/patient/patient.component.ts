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
import { AfterViewInit, ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { VaccinationRecordService } from '../../../entities/vaccintion-record/service/vaccination-record.service';
import { PerformerToStringPipe } from '../../pipes';

@Component({
  selector: 'vm-patient',
  standalone: true,
  imports: [CommonModule, PerformerToStringPipe],
  templateUrl: './patient.component.html',
  styleUrls: ['./patient.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PatientComponent implements AfterViewInit {
  patient$: Observable<string> | null = null;

  @Input() isMobile!: boolean;
  constructor(private record: VaccinationRecordService) {}

  ngAfterViewInit(): void {
    this.patient$ = this.record.getPatient().pipe(shareReplay(1));
  }
}

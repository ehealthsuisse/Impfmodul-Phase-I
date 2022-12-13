/**
 * Copyright (c) 2022 eHealth Suisse
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
import { AfterViewInit, ChangeDetectionStrategy, Component, inject, OnInit, ViewChild } from '@angular/core';
import { BaseHttpService, FormOptionsService } from '../../services';
import { SharedLibsModule } from '../../shared-libs.module';
import { IValueDTO } from '../../interfaces';
import { ReplaySubject } from 'rxjs';
import { MatSelect } from '@angular/material/select';
import { SharedDataService } from '../../services/shared-data.service';

@Component({
  selector: 'vm-confidentiality',
  standalone: true,
  imports: [SharedLibsModule, CommonModule],
  templateUrl: './confidentiality.component.html',
  styleUrls: ['./confidentiality.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfidentialityComponent implements OnInit, AfterViewInit {
  formOptionsService: FormOptionsService = inject(FormOptionsService);
  baseServices: BaseHttpService<any> = inject(BaseHttpService<any>);
  confidentialityOptions: Map<string, IValueDTO[]> = new Map<string, IValueDTO[]>();
  confidentiality: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  sharedDataService: SharedDataService = inject(SharedDataService);
  confidentialityWithOutSecret: ReplaySubject<IValueDTO[]> = new ReplaySubject<IValueDTO[]>(1);
  role: string = this.sharedDataService.storedData['role']!;
  defaultConfidentiality: ReplaySubject<IValueDTO> = new ReplaySubject<IValueDTO>(1);

  @ViewChild('confidentialityStatus', { static: true }) confidentialityStatus!: MatSelect;

  changeStatus(value: IValueDTO): void {
    this.baseServices.confidentialityStatus = value;
  }

  ngOnInit(): void {
    this.formOptionsService.getAllOptions().subscribe({
      next: options =>
        options.forEach(option => {
          if (option.name === 'confidentialityCode') {
            this.confidentialityOptions.set(option.name, option.entries!);
            this.defaultConfidentiality.next(
              this.confidentialityOptions.get('confidentialityCode')!.find(item => item.code === '17621005')!
            );
            this.confidentiality.next(this.confidentialityOptions.get('confidentialityCode')!.filter(item => item.code !== '17621005'));
            this.confidentialityWithOutSecret.next(
              this.confidentialityOptions.get('confidentialityCode')!.filter(item => item.name !== 'Secret' && item.code !== '17621005')
            );
          }
        }),
    });
  }

  ngAfterViewInit(): void {
    this.defaultConfidentiality.subscribe({
      next: value => {
        this.baseServices.confidentialityStatus = value;
      },
    });
  }
}

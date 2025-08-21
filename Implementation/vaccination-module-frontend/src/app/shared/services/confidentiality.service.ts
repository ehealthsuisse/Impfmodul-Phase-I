/**
 * Copyright (c) 2025 eHealth Suisse
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
import { Injectable } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormOptionsService } from './form-options.service';
import { map, Observable, ReplaySubject } from 'rxjs';
import { IValueDTO } from '../interfaces';

@Injectable({
  providedIn: 'root',
})
export class ConfidentialityService {
  constructor(private formOptionsService: FormOptionsService) {}

  getFilteredConfidentialityOptions(role: string): Observable<IValueDTO[]> {
    return this.formOptionsService.getAllOptions().pipe(
      map(options => {
        const confidentialityOption = options.find(opt => opt.name === 'confidentialityCode');
        if (!confidentialityOption?.entries) {
          return [];
        }

        return confidentialityOption.entries.filter(entry => {
          const isSecret = entry.name === 'Secret';
          return !isSecret || role === 'PAT' || role === 'REP';
        });
      })
    );
  }

  loadConfidentialityOptionsWithDefaultSelection(role: string, confidentialityList: ReplaySubject<IValueDTO[]>, editForm: FormGroup): void {
    this.getFilteredConfidentialityOptions(role).subscribe({
      next: filteredOptions => {
        confidentialityList.next(filteredOptions);

        const currentValue = editForm.get('confidentiality')?.value;
        const defaultOption = filteredOptions.find(opt => opt.code === '17621005');

        if (!currentValue && defaultOption) {
          editForm.get('confidentiality')?.setValue(defaultOption);
        }
      },
    });
  }
}

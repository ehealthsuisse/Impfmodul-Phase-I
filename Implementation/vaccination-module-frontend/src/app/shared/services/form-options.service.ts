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
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { IValueDTO } from '../interfaces';
import { VaccineWithDiseases } from '../interfaces/vaccination-with-disease.interface';
import { ConfigService } from '../../core/config/config.service';

@Injectable({ providedIn: 'root' })
export class FormOptionsService {
  http: HttpClient = inject(HttpClient);
  appConfig: ConfigService = inject(ConfigService);

  private _data = new Map<string, IValueDTO[]>();

  getOption(valuelistName: string, code: string): IValueDTO {
    let entries: IValueDTO[] = this._data.get(valuelistName) || [];
    let result = entries.find(entry => entry.code === code);
    if (result) {
      return result;
    } else {
      throw new Error('Option value not found');
    }
  }

  getAllOptions(): Observable<IValueDTO[]> {
    return this.http
      .get<IValueDTO[]>(`${this.appConfig.endpointPrefix}/utility/getAllValuesLists`)
      .pipe(tap(options => options.map(option => this._data.set(option.name, option.entries!))));
  }

  getVaccinationsWithDiseases(): Observable<VaccineWithDiseases[]> {
    return this.http.get<VaccineWithDiseases[]>(`${this.appConfig.endpointPrefix}/utility/vaccinesToTargetDiseases`);
  }
}

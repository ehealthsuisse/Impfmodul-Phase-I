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
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicationConfigService } from '../../core';
import { IValueListDTO } from '../interfaces/valueListDTO.interface';
import { IValueDTO } from '../interfaces';
import { VaccineWithDiseases } from '../interfaces/vaccination-with-disease.interface';

@Injectable({ providedIn: 'root' })
export class FormOptionsService {
  get data(): Map<string, IValueListDTO[]> {
    return this._data;
  }

  set data(value: Map<string, IValueListDTO[]>) {
    this._data = value;
  }
  http: HttpClient = inject(HttpClient);
  appConfig: ApplicationConfigService = inject(ApplicationConfigService);
  formOptions!: Observable<IValueListDTO[]>;

  private _data = new Map<string, IValueListDTO[]>();

  getAllOptions(): Observable<IValueDTO[]> {
    return this.http.get<IValueDTO[]>(`${this.appConfig.endpointPrefix}/utility/getAllValuesLists`);
  }

  getVaccinationsWithDiseases(): Observable<VaccineWithDiseases[]> {
    return this.http.get<VaccineWithDiseases[]>(`${this.appConfig.endpointPrefix}/utility/vaccinesToTargetDiseases`);
  }
}

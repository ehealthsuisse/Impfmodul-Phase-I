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
import { Observable, of, tap } from 'rxjs';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { ApplicationConfigService } from '../../core';
import { IBaseDTO, IValueDTO } from '../interfaces';
import { SpinnerService } from './spinner.service';
import { SharedDataService } from './shared-data.service';

@Injectable({
  providedIn: 'root',
})
export abstract class BaseHttpService<T extends IBaseDTO> {
  records: T[] = [];
  resource: string = 'communityIdentifier/GAZELLE/oid/1.3.6.1.4.1.21367.13.20.3000/localId/CHPAM4489';
  confidentialityStatus!: IValueDTO;
  protected http = inject(HttpClient);
  protected SpinnerService = inject(SpinnerService);
  protected sharedDataService: SharedDataService = inject(SharedDataService);
  protected readonly applicationConfig = inject(ApplicationConfigService);
  abstract prefix: string;

  query(): Observable<T[]> {
    return this.http
      .get<T[]>(`${this.applicationConfig.endpointPrefix}/${this.prefix}/${this.resource}`, { params: new HttpParams() })
      .pipe(tap(records => (this.records = records)));
  }

  create(entity: T): Observable<T> {
    this.SpinnerService.show();
    let t = {
      author: {
        firstName: this.sharedDataService.storedData['ufname']!,
        lastName: this.sharedDataService.storedData['ugname']!,
        prefix: this.sharedDataService.storedData['utitle']!,
        role: this.sharedDataService.storedData['role']!,
      },
      ...entity,
    };

    return this.http
      .post<T>(`${this.applicationConfig.endpointPrefix}/${this.prefix}/${this.resource}`, t)
      .pipe(tap(() => this.SpinnerService.hide()));
  }

  update(entity: T): Observable<T> {
    this.SpinnerService.show();

    return this.http
      .post<T>(`${this.applicationConfig.endpointPrefix}/${this.prefix}/${this.resource}/uuid/${entity.id}`, entity)
      .pipe(tap(() => this.SpinnerService.hide()));
  }

  delete(id: string): Observable<HttpResponse<{}>> {
    return this.http
      .delete(`${this.applicationConfig.endpointPrefix}/${this.prefix}/${this.resource}/uuid/${id}`, { observe: 'response' })
      .pipe(tap(() => this.SpinnerService.hide()));
  }

  find(id?: string): Observable<T> {
    return of(this.records.find(e => e.id === id)!);
  }

  validate(entity: T): Observable<T> {
    let t = {
      ...entity,
    };
    return this.http
      .post<T>(`${this.applicationConfig.endpointPrefix}/${this.prefix}/validate/${this.resource}/uuid/${t.id}`, t)
      .pipe(tap(() => this.SpinnerService.hide()));
  }
}

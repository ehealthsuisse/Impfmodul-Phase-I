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
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import dayjs from 'dayjs';
import { IVaccination } from '../../../model/vaccination.interface';
import { VaccinationService } from './vaccination.service';
import { RouterTestingModule } from '@angular/router/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('Vaccination Service', () => {
  let service: VaccinationService;
  let httpMock: HttpTestingController;
  let elemDefault: IVaccination;
  let expectedResult: IVaccination | IVaccination[] | boolean | null;
  let currentDate: dayjs.Dayjs;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [VaccinationService, RouterTestingModule, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(VaccinationService);
    httpMock = TestBed.inject(HttpTestingController);
    currentDate = dayjs();

    elemDefault = {
      id: '1',
      vaccineCode: { code: '', name: 'AAAAAAA' },
      code: { code: '', name: 'AAAAAAA' },
      confidentiality: { code: 'AAAAA', name: 'AAAAA' },
      targetDiseases: [{ code: '', name: 'AAAAAAA' }],
      doseNumber: '1',
      occurrenceDate: currentDate,
      performer: { firstName: 'AAAAAAA', lastName: 'AAAAAAA', prefix: 'AAAAAAA' },
      lotNumber: 'AAAAAAA',
      reason: { code: '', name: 'AAAAAAA' },
      status: { code: '', name: 'AAAAAAA' },
      validated: true,
      deleted: false,
      relatedId: 'AAAAAA',
      organization: 'AAAAAA',
      author: { firstName: 'AAAAAAA', lastName: 'AAAAAAA', prefix: 'AAAAAAA' },
      updated: false,
      content: ['AAAAA'],
    };
  });

  // FIXME: This test is failing because there is No records array  to get data from
  xdescribe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = Object.assign(
        {
          occurrenceDate: currentDate.format('dd/MM/yyyy'),
        },
        elemDefault
      );
      service.find('123');

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toEqual(elemDefault);
    });

    it('should create a Vaccination', () => {
      const returnedFromService = Object.assign(
        {
          id: '0',
          time: currentDate.format('dd/MM/yyyy'),
        },
        elemDefault
      );

      const expected = Object.assign(
        {
          occurrenceDate: currentDate,
        },
        returnedFromService
      );

      service.create({} as IVaccination).subscribe(resp => (expectedResult = resp));
      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toEqual(expected);
    });

    it('should update a Vaccination', () => {
      const returnedFromService = Object.assign(
        {
          id: '1',
          vaccineCode: { display: 'BBBBBB', code: 'BBBBBB' },
          targetDiseases: [{ display: 'BBBBBB', code: null }],
          doseNumber: 1,
          occurrenceDate: currentDate.format('dd/MM/yyyy'),
          performer: { firstName: 'BBBBBB', lastName: 'BBBBBB', prefix: 'BBBBBB' },
          lotNumber: 'BBBBBB',
          reasons: { display: 'BBBBBB', code: 'BBBBBB' },
          status: { code: '', name: 'AAAAAAA' },
          validated: true,
          deleted: false,
          relatedId: 'BBBBBB',
          organization: 'BBBBBB',
        },
        elemDefault
      );

      const expected = Object.assign(
        {
          occurrenceDate: currentDate,
        },
        returnedFromService
      );

      service.update(expected).subscribe(resp => (expectedResult = resp));
      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toEqual(expected);
    });

    it('should return a list of Vaccination', () => {
      const returnedFromService = Object.assign(
        {
          id: '1',
          vaccineCode: { code: '', display: 'AAAAAAA' },
          targetDiseases: [{ code: '', display: 'AAAAAAA' }],
          doseNumber: '1',
          occurrenceDate: currentDate.format('dd/MM/yyyy'),
          performer: { firstName: 'AAAAAAA', lastName: 'AAAAAAA', prefix: 'AAAAAAA' },
          lotNumber: 'AAAAAAA',
          reasons: { code: '', display: 'AAAAAAA' },
          status: 'AAAAAAA',
          validated: true,
          deleted: true,
          relatedId: 'AAAAAAA',
          organization: 'AAAAAAA',
        },
        elemDefault
      );

      const expected = Object.assign(
        {
          occurrenceDate: currentDate.format('dd/MM/yyyy'),
        },
        returnedFromService
      );
      let returnedValue;

      service.query().subscribe(resp => (returnedValue = resp));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(returnedValue).toContain(expected);
    });

    it('should delete a Vaccination', () => {
      service.deleteWithBody('123').subscribe((resp: any) => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(true);
    });
  });
});

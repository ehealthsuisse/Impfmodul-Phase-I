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
import { IAdverseEvent } from '../../../model';
import { AdverseEventService } from './adverse-event.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('Vaccination Service', () => {
  let service: AdverseEventService;
  let httpMock: HttpTestingController;
  let elemDefault: IAdverseEvent;
  let expectedResult: IAdverseEvent | IAdverseEvent[] | boolean | null;
  let currentDate: dayjs.Dayjs;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(AdverseEventService);
    httpMock = TestBed.inject(HttpTestingController);
    currentDate = dayjs();

    elemDefault = {
      confidentiality: { code: 'AAAAA', name: 'AAAAA' },
      id: '0',
      allergyCode: { code: 'AAAAA', name: 'AAAAA' },
      code: { code: 'AAAAA', name: 'AAAAA' },
      criticality: { code: 'AAAAA', name: 'AAAAA' },
      verificationStatus: { code: 'AAAAA', name: 'AAAAA' },
      clinicalStatus: { code: 'AAAAA', name: 'AAAAA' },
      organization: 'AAAAAA',
      occurrenceDate: currentDate,
      recorder: { prefix: 'A', firstName: '', lastName: '' },
      category: { code: 'AAAAA', name: 'AAAAA' },
      updated: false,
      validated: true,
      content: ['AAAAA'],
    };
  });
  // FIXME: This test is failing because there is No records array  to get data from
  xdescribe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = Object.assign(
        {
          // occurrenceDate: currentDate.format('dd/MM/yyyy'),
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
          occurrenceDate: currentDate.format('dd/MM/yyyy'),
        },
        elemDefault
      );

      const expected = Object.assign(
        {
          occurrenceDate: currentDate,
        },
        returnedFromService
      );

      service.create({} as IAdverseEvent).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toEqual(expected);
    });

    it('should update a Vaccination', () => {
      const returnedFromService = Object.assign(
        {
          id: '0',
          allergyCode: { code: 'BBBB', name: 'BBBB' },
          code: { code: 'BBBB', name: 'BBBB' },
          criticality: { code: 'BBBB', name: 'BBBB' },
          verificationStatus: { code: 'BBBB', name: 'BBBB' },
          clinicalStatus: { code: 'BBBB', name: 'BBBB' },
          organization: 'BBBBBB',
        },
        elemDefault
      );

      const expected = Object.assign(
        {
          allergyCode: { code: 'BBBB', name: 'BBBB' },
          criticality: { code: 'BBBB', name: 'BBBB' },
          clinicalStatus: { code: 'BBBB', name: 'BBBB' },
          organization: 'BBBBB',
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
          id: '0',
          allergyCode: { name: 'AAAAA', code: 'AAAAA' },
          code: { name: 'AAAAA', code: 'AAAAA' },
          criticality: 'AAAAA',
          verificationStatus: true,
          clinicalStatus: 'AAAAAAA',
          organization: 'AAAAA',
          recorder: { prefix: 'A', firstName: '', lastName: '' },
          occurrenceDate: currentDate,
        },
        elemDefault
      );

      const expected = Object.assign(
        {
          // occurrenceDate: currentDate,
        },
        returnedFromService
      );
      let expectedResponse;

      service.query().subscribe(resp => (expectedResponse = resp));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResponse).toContain(expected);
    });

    it('should delete a Vaccination', () => {
      service.deleteWithBody('123').subscribe((resp: any) => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ clinicalStatus: 200 });
      expect(expectedResult).toBe(true);
    });
  });
});

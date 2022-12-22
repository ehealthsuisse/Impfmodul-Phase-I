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
import dayjs from 'dayjs';
import { IBaseDTO } from '../shared/interfaces/baseDTO.interface';
import { IValueDTO } from '../shared/interfaces/valueDTO.interface';
import { IHumanDTO } from '../shared/interfaces/humanDTO.interface';
import { Author } from '../shared';

export interface Vaccination extends IBaseDTO {
  targetDiseases: IValueDTO[];
  doseNumber: string;
  occurrenceDate: dayjs.Dayjs | string;
  lotNumber: string | null;
  reason: IValueDTO;
  status: IValueDTO;
  validated: boolean | null;
  vaccineCode: IValueDTO;
  performer: IHumanDTO;
  author?: Author;
}

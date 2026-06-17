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
import { FormatBasedOnTypePipe } from './format-based-on-type.pipe';
import dayjs from 'dayjs';
import { DATE_FORMAT } from '../date';

describe('FormatBasedOnTypePipe', () => {
  let pipe: FormatBasedOnTypePipe;

  beforeEach(() => {
    pipe = new FormatBasedOnTypePipe();
  });

  it('should create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should format a dayjs object using DATE_FORMAT', () => {
    const date = dayjs('2024-06-01');
    const formatted = pipe.transform(date);
    expect(formatted).toBe(date.format(DATE_FORMAT.parse.dateInput));
  });

  it('should return the value unchanged if not a dayjs object', () => {
    const value = 'not a date';
    expect(pipe.transform(value as any)).toBe(value);
  });
});

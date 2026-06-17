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
import { PerformerToStringPipe } from './performer-to-string.pipe';
import { IHumanDTO } from '../interfaces';

describe('PerformerToStringPipe', () => {
  let pipe: PerformerToStringPipe;

  beforeEach(() => {
    pipe = new PerformerToStringPipe();
  });

  it('should create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should format all fields', () => {
    const user: IHumanDTO = { prefix: 'Dr.', firstName: 'Max', lastName: 'Mustermann' };
    expect(pipe.transform(user)).toBe('Dr. Max Mustermann');
  });

  it('should handle missing prefix', () => {
    const user: IHumanDTO = { firstName: 'Max', lastName: 'Mustermann' };
    expect(pipe.transform(user)).toBe('Max Mustermann');
  });

  it('should handle fields as "null" string', () => {
    const user: IHumanDTO = { prefix: 'null', firstName: 'null', lastName: 'null' };
    expect(pipe.transform(user)).toBe('');
  });

  it('should return empty string for undefined', () => {
    expect(pipe.transform(undefined)).toBe('');
  });
});

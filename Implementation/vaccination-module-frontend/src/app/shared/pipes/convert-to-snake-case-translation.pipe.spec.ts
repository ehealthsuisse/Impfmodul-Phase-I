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
import { ConvertToSnakeCaseTranslationPipe } from './convert-to-snake-case-translation.pipe';

describe('ConvertToSnakeCaseTranslationPipe', () => {
  let pipe: ConvertToSnakeCaseTranslationPipe;

  beforeEach(() => {
    pipe = new ConvertToSnakeCaseTranslationPipe();
  });

  it('should create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should convert camelCase to SNAKE_CASE with prefix', () => {
    expect(pipe.transform('camelCaseInput', 'PREFIX_')).toBe('PREFIX_CAMEL_CASE_INPUT');
  });

  it('should handle single word input', () => {
    expect(pipe.transform('word', 'PRE_')).toBe('PRE_WORD');
  });

  it('should handle empty string input', () => {
    expect(pipe.transform('', 'PRE_')).toBe('PRE_');
  });

  it('should handle input with no uppercase letters', () => {
    expect(pipe.transform('lowercase', 'PRE_')).toBe('PRE_LOWERCASE');
  });

  it('should handle input with special characters', () => {
    expect(pipe.transform('someValue123', 'PRE_')).toBe('PRE_SOME_VALUE123');
  });

  it('should handle prefix as empty string', () => {
    expect(pipe.transform('someValue', '')).toBe('SOME_VALUE');
  });
});

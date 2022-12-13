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
import { ReplaySubject, take } from 'rxjs';
import { IHumanDTO } from '../interfaces/humanDTO.interface';
import { IBaseDTO, IValueDTO } from '../interfaces';
import { MatSelect } from '@angular/material/select';
import { FormControl } from '@angular/forms';
import '../../core/config/dayjs';

/*
 * @param humanDTO
 * @returns human as a string
 */
export function humanToString(value: IHumanDTO): IHumanDTO {
  if (value?.prefix || value?.firstName || value?.lastName) {
    return `${value?.prefix} ${value?.firstName} ${value?.lastName}` as unknown as IHumanDTO;
  } else {
    return '' as unknown as IHumanDTO;
  }
}

/*
  @param list
  used for drop down list to set initial value
 */
export function setDropDownInitialValue(list: ReplaySubject<IValueDTO[]>, selectedOption: MatSelect): void {
  list.pipe(take(1)).subscribe(() => {
    selectedOption.compareWith = (a: IValueDTO, b: IValueDTO) => a && b && a.code === b.code;
  });
}
/*
  @param list - filtered - control
  used to filter dropdown list
 */
export function filterDropdownList(list: IValueDTO[], filteredList: ReplaySubject<IValueDTO[]>, control: FormControl): void {
  if (!list) {
    return;
  }
  let searchKeyWord = control.value;
  if (!searchKeyWord) {
    filteredList.next(list.slice());
    return;
  }
  searchKeyWord = searchKeyWord.toLowerCase();
  filteredList.next(list.filter(vaccine => vaccine.name.toLowerCase().indexOf(searchKeyWord) > -1));
}

export function downloadRecordValue<T extends IBaseDTO>(t: T, patient: IHumanDTO): void {
  let element = document.createElement('a');
  element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(t.json!));
  element.setAttribute('download', patient.lastName + '.txt');
  element.style.display = 'none';
  document.body.appendChild(element);
  element.click();
  document.body.removeChild(element);
}

export const parseStringToDate = (input: string): Date | null => {
  const dateRegex = /^(0[1-9]|1\d|2\d|3[01])\.(0[1-9]|1[0-2])\.(19|20)\d{2}$/;

  if (!dateRegex.test(input)) {
    return null;
  }

  const differentFormatDate = input.split('.').reverse().join('-');

  return new Date(differentFormatDate);
};

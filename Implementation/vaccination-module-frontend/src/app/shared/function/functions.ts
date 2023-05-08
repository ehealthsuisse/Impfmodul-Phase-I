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
import { FormControl } from '@angular/forms';
import { MatSelect } from '@angular/material/select';
import { ReplaySubject, take } from 'rxjs';
import '../date/dayjs';
import { IBaseDTO, IValueDTO } from '../interfaces';
import { IHumanDTO } from '../interfaces/humanDTO.interface';
import { Type } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { SharedDataService } from '../services/shared-data.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { SessionInfoService } from '../../core/security/session-info.service';
import dayjs from 'dayjs';
import { DATE_FORMAT } from '../date';

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
    if (selectedOption) {
      selectedOption.compareWith = (a: IValueDTO, b: IValueDTO) => a && b && a.code === b.code;
    }
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

export function downloadRecordValue<T extends IBaseDTO>(t: T, patient: IHumanDTO, sessionInfoService: SessionInfoService): void {
  let element = document.createElement('a');
  element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(t.json!));
  const { prefix, lastName, firstName } = sessionInfoService.author.getValue();
  const fileName = `${prefix ? prefix + '_' : ''}${lastName ? lastName + '_' : ''}${firstName ? firstName + '_' : ''}${dayjs().format(
    DATE_FORMAT
  )}.json`;
  element.setAttribute('download', fileName);
  element.style.display = 'none';
  document.body.appendChild(element);
  element.click();
  document.body.removeChild(element);
}

export function deleteRecord<T>(matDialog: MatDialog, dialogComponent: Type<any>, service: any, baseDetails: T, details: any): void {
  const dialogData = {
    value: { ...baseDetails },
    button: 'buttons.DELETE',
  };
  const dialogConfig = {
    width: '60vw',
    data: dialogData,
  };

  // eslint-disable-next-line security/detect-non-literal-fs-filename
  matDialog
    .open(dialogComponent, dialogConfig)
    .afterClosed()
    .subscribe(confirmed => {
      if (confirmed) {
        service.deleteWithBody(details.id!, details.confidentiality).subscribe(() => window.history.back());
      }
    });
}

export function initializeActionData(type: string, sharedData: SharedDataService): void {
  if (type === 'details') {
    sharedData.detailedActions = true;
    sharedData.patientActions = false;
  } else if (type === 'record') {
    sharedData.detailedActions = false;
    sharedData.patientActions = true;
  } else {
    sharedData.detailedActions = false;
    sharedData.patientActions = false;
  }
}

export const parseStringToDate = (input: string | null): Date | null => {
  const dateRegex = /^(0[1-9]|1\d|2\d|3[01])\.(0[1-9]|1[0-2])\.(19|20)\d{2}$/;

  if (input == null || !dateRegex.test(input)) {
    return null;
  }

  const differentFormatDate = input.split('.').reverse().join('-');

  return new Date(differentFormatDate);
};

/**
 * Overwrites the default filterPredicate from Angular by removing the JSON field.
 * This is necessary because one json could contain multiple entries which would then be found even
 * if only one of the entries are refered to.
 */
export const filterPredicateExcludeJSONField = (data: any, filter: string): boolean => {
  // Transform the data into a lowercase string of all property values.
  const dataStr = Object.keys(data)
    .reduce((currentTerm: string, key: string) => {
      // ignore json field
      if (key === 'json') {
        return currentTerm;
      }

      // Use an obscure Unicode character to delimit the words in the concatenated string.
      // This avoids matches where the values of two columns combined will match the user's query
      // (e.g. `Flute` and `Stop` will match `Test`). The character is intended to be something
      // that has a very low chance of being typed in by somebody in a text field. This one in
      // particular is "White up-pointing triangle with dot" from
      // https://en.wikipedia.org/wiki/List_of_Unicode_characters
      return currentTerm + (data as { [key: string]: any })[`${key}`] + '◬';
    }, '')
    .toLowerCase();

  // Transform the filter by converting it to lowercase and removing whitespace.
  const transformedFilter = filter.trim().toLowerCase();

  return dataStr.indexOf(transformedFilter) >= 0;
};

export function openSnackBar(translate: TranslateService, snackBar: MatSnackBar, body: string): void {
  // eslint-disable-next-line security/detect-non-literal-fs-filename
  snackBar.open(translate.instant(body), 'Dismiss', {
    duration: 2500,
    horizontalPosition: 'center',
    verticalPosition: 'top',
  });
}

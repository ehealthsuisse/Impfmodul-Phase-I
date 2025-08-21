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
import { FormControl, FormGroup } from '@angular/forms';
import { MatSelect } from '@angular/material/select';
import { ReplaySubject, take } from 'rxjs';
import '../date/dayjs';
import { IBaseDTO, IComment, IValueDTO } from '../interfaces';
import { IHumanDTO } from '../interfaces/humanDTO.interface';
import { MatDialog } from '@angular/material/dialog';
import { SharedDataService } from '../services/shared-data.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { SessionInfoService } from '../../core/security/session-info.service';
import dayjs from 'dayjs';
import { DATE_FORMAT } from '../date';
import { Router } from '@angular/router';
import { PatientService } from '../component/patient/patient.service';
import { ReusableDialogComponent } from '../component/resuable-fields/reusable-dialog/reusable-dialog.component';

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

export function downloadRecordValue<T extends IBaseDTO>(t: T, patientService: PatientService, url: string, docExtension: string): void {
  let link = document.createElement('a');
  link.href = url;
  const patient = patientService.patient.getValue();
  link.download = `${patient.prefix ? patient.prefix + '_' : ''}${patient.lastName ? patient.lastName + '_' : ''}${
    patient.firstName ? patient.firstName + '_' : ''
  }${dayjs().format(DATE_FORMAT.parse.dateInput)}${docExtension}`;
  link.style.display = 'none';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

export function deleteRecord(matDialog: MatDialog, service: any, details: IBaseDTO, deleteMessage: string): void {
  matDialog
    .open(ReusableDialogComponent, {
      data: {
        message: deleteMessage,
      },
    })
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
  snackBar.open(translate.instant(body), 'Dismiss', {
    duration: 2500,
    horizontalPosition: 'center',
    verticalPosition: 'top',
  });
}

export function routecall(router: Router, sessionInfoService: SessionInfoService, extension: string): void {
  const routeToVaccinationRecord = sessionInfoService.isFromVaccinationRecord;
  sessionInfoService.isFromVaccinationRecord = false;
  if (!routeToVaccinationRecord) {
    router.navigate([extension]);
  } else {
    router.navigate(['/vaccination-record']);
  }
}

export function buildComment(commentText: string, author = 'will be added by the system'): IComment | undefined {
  const trimmed = commentText?.trim();
  return trimmed ? { text: trimmed, author } : undefined;
}

// function used when resetting a form to provide default values
export function setDefaultValues(form: FormGroup, fieldName: string, value: any): void {
  const control = form.get(fieldName);
  if (control) {
    control.setValue(value);
  }
}

export function extractSessionDetailsByRole(sessionInfo: SessionInfoService): {
  validated: boolean;
  firstName: string;
  lastName: string;
  prefix: string;
  organization: string;
} {
  const role = sessionInfo.queryParams.role;
  const isHCP = role === 'HCP';
  const validated = isHCP || role === 'ASS';

  const authorInfo = sessionInfo.author.getValue();
  const firstName = isHCP ? authorInfo.firstName : '';
  const lastName = isHCP ? authorInfo.lastName : '';
  const prefix = isHCP ? authorInfo.prefix ?? '' : '';
  const organization = sessionInfo.queryParams.organization;

  return { validated, firstName, lastName, prefix, organization };
}

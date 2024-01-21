import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '../../services';
import { IBaseDTO } from '../../interfaces';

export function openErrorDialog<T extends IBaseDTO>(
  title: string,
  body: string,
  footer: string,
  translateService: TranslateService,
  dialogService: DialogService,
  object: T
): void {
  const dialogTitle = translateService.instant(title);
  const dialogBody = createErrorDialogBody(body, translateService);
  const dialogFooter = translateService.instant(footer);
  const dialogMessage = `${dialogBody} <br> ${dialogFooter}`;
  dialogService.openDialog(dialogTitle, dialogMessage, true, object);
}

export function openVaccinationRecordErrorDialog(
  title: string,
  body: string,
  footer: string,
  translateService: TranslateService,
  dialogService: DialogService,
  allPatientData: IBaseDTO[]
): void {
  const dialogTitle = translateService.instant(title);
  const dialogBody = vaccinationRecordErrorDialog(body, translateService, allPatientData);
  const dialogFooter = translateService.instant(footer);
  const dialogMessage = `${dialogBody} <br> ${dialogFooter}`;
  dialogService.openDialog(dialogTitle, dialogMessage, false);
}

export function createErrorDialogBody(errorBody: string, translateService: TranslateService): string {
  const bodyHeader = translateService.instant(errorBody);
  return `${bodyHeader}`;
}

export function vaccinationRecordErrorDialog(errorBody: string, translateService: TranslateService, allPatientData: IBaseDTO[]): string {
  const bodyHeader = translateService.instant(errorBody);
  const filteredData = allPatientData.filter(e => e.hasErrors);
  
  // remove duplicates
  const uniqueData = filteredData.reduce((b: IBaseDTO[], a) => {
    let i = b.findIndex(e => e.json === a.json)
    if (i === -1) {
      b.push(a)
    }
    return b;
  }, []);
  const filteredCount = uniqueData.length;

  const dynamicErrorMessage = translateService.instant(bodyHeader);
  const errorMessageBody = dynamicErrorMessage.replace('{{count}}', filteredCount.toString(), 'g');

  return `${errorMessageBody}<br>`;
}

import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function dateValidator(): ValidatorFn {
  return (formControl: AbstractControl): ValidationErrors | null => {
    const formGroup = formControl.parent;

    if (formGroup) {
      const beginDate =
        formGroup?.get('begin')?.value instanceof Date ? formGroup.get('begin')?.value : new Date(formGroup?.get('begin')?.value);

      const endDate = formControl.value instanceof Date ? formControl.value : formControl.value ? new Date(formControl.value) : null;

      if (endDate !== null && endDate <= beginDate) {
        return { endDateBeforeStartDate: true };
      }
    }
    return null;
  };
}

export function notFutureDateValidator(dateFieldName: string): ValidatorFn {
  return (formControl: AbstractControl): ValidationErrors | null => {
    const formGroup = formControl.parent;
    const currentDate = new Date();

    if (formGroup) {
      const dateValue = formGroup.get(dateFieldName)?.value;
      const date = dateValue instanceof Date ? dateValue : new Date(dateValue);
      if (date > currentDate) {
        return { futureDate: true };
      }
    }

    return null;
  };
}

import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function dateValidator(startDateField: string, endDateField: string): ValidatorFn {
  return (formControl: AbstractControl): ValidationErrors | null => {
    const formGroup = formControl.parent;

    if (formGroup) {
      const startDateValue = formGroup.get(startDateField)?.value;
      const endDateValue = formGroup.get(endDateField)?.value;

      if (startDateValue && endDateValue) {
        const startDate = new Date(startDateValue);
        const endDate = new Date(endDateValue);

        if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
          return { invalidDate: true };
        }

        const normalizedStartDate = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate());
        const normalizedEndDate = new Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate());

        if (normalizedEndDate <= normalizedStartDate) {
          return { endDateBeforeStartDate: true };
        }
      }
    }
    return null;
  };
}

export function validateDatesNotBefore(startDateFieldName: string, endDateFieldName: string): ValidatorFn {
  return (formControl: AbstractControl): ValidationErrors | null => {
    const formGroup = formControl.parent;

    if (formGroup) {
      const startDateValue = formGroup.get(startDateFieldName)?.value;
      const endDateValue = formGroup.get(endDateFieldName)?.value;

      if (!startDateValue || !endDateValue) {
        return null;
      }

      const startDateInstance = startDateValue instanceof Date ? startDateValue : new Date(startDateValue);
      const endDateInstance = endDateValue instanceof Date ? endDateValue : new Date(endDateValue);

      // Normalize both dates to ignore time parts
      const normalizedStartDate = new Date(startDateInstance.getFullYear(), startDateInstance.getMonth(), startDateInstance.getDate());
      const normalizedEndDate = new Date(endDateInstance.getFullYear(), endDateInstance.getMonth(), endDateInstance.getDate());

      if (normalizedStartDate.getTime() < normalizedEndDate.getTime()) {
        return { startDateBeforeEndDate: true };
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

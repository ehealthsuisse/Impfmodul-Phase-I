import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function dateValidator(): ValidatorFn {
  return (formControl: AbstractControl): ValidationErrors | null => {
    const formGroup = formControl.parent;

    if (formGroup) {
      const beginDate = formGroup.get('begin')?.value;
      const endDate = formControl.value;

      if (endDate && endDate <= beginDate) {
        return { endDateBeforeStartDate: true };
      }
    }

    return null;
  };
}

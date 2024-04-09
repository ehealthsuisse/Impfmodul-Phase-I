import { notFutureDateValidator } from 'src/app/core/validators/date-order-validator'; // Import the validator function
import { FormControl, FormGroup } from '@angular/forms';

describe('notFutureDateValidator', () => {
  it('should return a validation error if the date is in the future', () => {
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 1); // Set a date in the future

    const validatorFn = notFutureDateValidator('testDate');
    const formGroup = new FormGroup({
      testDate: new FormControl(futureDate),
    });

    const validationErrors = validatorFn(formGroup.controls['testDate']);

    expect(validationErrors).toEqual({ futureDate: true });
  });

  it('should not return a validation error if the date is in the past or present', () => {
    const pastDate = new Date();
    pastDate.setDate(pastDate.getDate() - 1); // Set a date in the past

    const validatorFn = notFutureDateValidator('testDate');
    const formGroup = new FormGroup({
      testDate: new FormControl(pastDate),
    });

    const validationErrors = validatorFn(formGroup.controls['testDate']);

    expect(validationErrors).toBeNull();
  });
});

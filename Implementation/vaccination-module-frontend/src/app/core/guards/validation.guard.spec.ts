import { TestBed } from '@angular/core/testing';

import { ValidationGuard } from './validation.guard';
import { SharedTestingModule } from '../../shared/shared-testing.module';

describe('ValidationGuard', () => {
  let guard: ValidationGuard;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ValidationGuard],
      imports: [SharedTestingModule],
    });
    guard = TestBed.inject(ValidationGuard);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });
});

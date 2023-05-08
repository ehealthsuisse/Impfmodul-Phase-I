import { TestBed } from '@angular/core/testing';

import { SamlInterceptor } from './saml.interceptor';

describe('SamlInterceptor', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [SamlInterceptor],
    })
  );

  it('should be created', () => {
    const interceptor: SamlInterceptor = TestBed.inject(SamlInterceptor);
    expect(interceptor).toBeTruthy();
  });
});

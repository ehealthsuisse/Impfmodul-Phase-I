import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { ConfigService } from '../config/config.service';
import { ValidationService } from '../security/validation.service';
import { CryptoJsService } from '../security/crypto-js.service';

@Injectable({
  providedIn: 'root',
})
export class ValidationGuard implements CanActivate {
  constructor(
    private configService: ConfigService,
    private validationService: ValidationService,
    private router: Router,
    private cryptoService: CryptoJsService
  ) {}
  canActivate(): boolean {
    const decryptedStatus = this.cryptoService.decryptValidationStatus();
    if (
      (decryptedStatus === true && this.validationService.validate(this.configService.endpointPrefix)) ||
      this.configService.canActivate
    ) {
      return true;
    } else {
      this.router.navigate(['/access-denied']);
      return false;
    }
  }
}

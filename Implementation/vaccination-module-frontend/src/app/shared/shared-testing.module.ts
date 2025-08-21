import { NgModule } from '@angular/core';
import { SharedLibsModule } from './shared-libs.module';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { MatDialogModule } from '@angular/material/dialog';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

@NgModule({
  exports: [NoopAnimationsModule, TranslateModule, MatDialogModule, SharedLibsModule],
  imports: [NoopAnimationsModule, TranslateModule.forRoot(), MatDialogModule],
  providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
})
export class SharedTestingModule {}

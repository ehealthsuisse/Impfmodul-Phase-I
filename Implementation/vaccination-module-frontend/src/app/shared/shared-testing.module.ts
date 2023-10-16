import { NgModule } from '@angular/core';
import { SharedLibsModule } from './shared-libs.module';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { MatDialogModule } from '@angular/material/dialog';
import { HttpClientTestingModule } from '@angular/common/http/testing';

@NgModule({
  imports: [RouterTestingModule, NoopAnimationsModule, TranslateModule.forRoot(), MatDialogModule, HttpClientTestingModule],
  exports: [NoopAnimationsModule, TranslateModule, MatDialogModule, HttpClientTestingModule, SharedLibsModule, RouterTestingModule],
})
export class SharedTestingModule {}

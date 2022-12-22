/**
 * Copyright (c) 2022 eHealth Suisse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the “Software”), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TableWrapperComponent } from './table-wrapper.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { SharedLibsModule } from '../../shared-libs.module';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Vaccination } from '../../../model/vaccination.interface';

describe('TableWrapperComponent', () => {
  let component: TableWrapperComponent<Vaccination>;
  let fixture: ComponentFixture<TableWrapperComponent<Vaccination>>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TableWrapperComponent, NoopAnimationsModule, SharedLibsModule, TranslateModule],
      providers: [
        {
          provide: TranslateService,
          useValue: jasmine.createSpyObj('TranslateService', ['get']),
        },
      ],
    });

    fixture = TestBed.overrideTemplate(TableWrapperComponent, '').createComponent(TableWrapperComponent<Vaccination>);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

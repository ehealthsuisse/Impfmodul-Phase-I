/**
 * Copyright (c) 2023 eHealth Suisse
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
import { FormControl } from '@angular/forms';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { map, Observable, startWith } from 'rxjs';
import { VaccinationFormGroup } from '../../services/vaccination-form.service';
import { IValueDTO } from '../../../../shared';
import { TranslateService } from '@ngx-translate/core';

export class ChipsHandler {
  list: IValueDTO[] = [];
  selected: IValueDTO[] = [];
  filtered!: Observable<IValueDTO[]>;
  ctrl = new FormControl('');
  private type: 'reason' | 'disease';
  constructor(type: 'reason' | 'disease', private translateService: TranslateService) {
    this.type = type;
  }
  select(item: MatAutocompleteSelectedEvent, form: VaccinationFormGroup): void {
    const i = item.option.value;
    if (this.selected.filter((x: any) => x.code === i.code).length) {
      this.ctrl.setValue(null);
      this.remove(i, form);
      return;
    }
    const formCtrl = this.getFormCtrl(form);
    this.selected = Array.isArray(formCtrl.value) ? [...formCtrl.value, i] : [i];
    if (this.list.length > 0) {
      this.list.filter(x => x.code === i.code)[0].selected = true;
    }
    formCtrl.setValue(this.selected);
    this.ctrl.setValue(null);
  }
  assign(form: VaccinationFormGroup): void {
    const formCtrl = this.getFormCtrl(form);
    this.selected = formCtrl.value;
    formCtrl.setValue(this.selected);
    this.ctrl.setValue(null);
    this.selected.forEach(element => {
      if (this.list.length > 0) {
        this.list.filter(x => x.code === element.code)[0].selected = true;
      }
    });
    this.filtered = this.ctrl.valueChanges.pipe(
      startWith(null),
      map((x: string | null) => (x ? this._filter(x, this.list) : this.list.slice()))
    );
  }
  remove(item: IValueDTO, form: VaccinationFormGroup): void {
    const formCtrl = this.getFormCtrl(form);
    this.selected = this.selected.filter((x: any) => x.code !== item.code);
    this.list.filter(x => x.code === item.code)[0].selected = false;
    formCtrl.setValue(this.selected);
    this.ctrl.setValue(null);
  }
  setupChipsControls(list: IValueDTO[]): void {
    this.list = list;
    this.filtered = this.ctrl.valueChanges.pipe(
      startWith(null),
      map((i: string | null) => (i ? this._filter(i, this.list) : this.list.slice()))
    );
  }
  private getFormCtrl(form: VaccinationFormGroup): FormControl {
    return this.type === 'reason' ? form.controls.reason : form.controls.targetDiseases;
  }
  private _filter(value: any, array: IValueDTO[]): IValueDTO[] {
    const filterValue = value.name ? value.name.toLowerCase() : value.toLowerCase();
    return array.filter(i => {
      const translatedName = this.translateService.instant('vaccination-targetdiseases.' + i.code);
      return translatedName.toLowerCase().includes(filterValue);
    });
  }
}

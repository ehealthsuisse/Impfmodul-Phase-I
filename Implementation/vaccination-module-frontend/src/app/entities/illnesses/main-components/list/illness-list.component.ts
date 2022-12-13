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
import { Component, inject, Input, OnInit } from '@angular/core';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { combineLatest } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import {
  DialogService,
  GenericButtonComponent,
  HelpButtonComponent,
  ListHeaderComponent,
  MainWrapperComponent,
  MapperService,
  PageTitleTranslateComponent,
  TableWrapperComponent,
  trackLangChange,
} from '../../../../shared';
import { Router } from '@angular/router';
import { ApplicationConfigService } from '../../../../core';
import { IIllnesses } from '../../../../model';
import { IllnessService } from '../../service/illness.service';

@Component({
  selector: 'vm-illnesses-list',
  templateUrl: './illness-list.component.html',
  styleUrls: ['./illness-list.component.scss'],
  standalone: true,
  imports: [
    SharedLibsModule,
    MatTableModule,
    TranslateModule,
    GenericButtonComponent,
    HelpButtonComponent,
    ListHeaderComponent,
    TableWrapperComponent,
    MainWrapperComponent,
  ],
})
export class IllnessListComponent extends PageTitleTranslateComponent implements OnInit {
  router: Router = inject(Router);
  dialog = inject(DialogService);
  appConfig = inject(ApplicationConfigService);
  illnesses!: MatTableDataSource<IIllnesses>;
  illnessesService: IllnessService = inject(IllnessService);
  illnessData$ = combineLatest([this.illnessesService.query(), trackLangChange()]);
  mapper: MapperService = inject(MapperService);

  @Input() toggleHeader: boolean = true;
  @Input() displayedColumns: string[] = ['entryStatus', 'recordedDate', 'illnessCode', 'clinicalStatus', 'verificationStatus', 'recorder'];
  @Input() buttonVisibility: boolean = true;
  @Input() subtitleVisibility: boolean = true;
  @Input() isEmbedded: boolean = false;
  @Input() showPatientName: boolean = true;
  @Input() isUpdated: boolean = false;
  @Input() tableWidth: string = '80vw';

  ngOnInit(): void {
    this.illnessData$.subscribe(([v]) => (this.illnesses = new MatTableDataSource<IIllnesses>(this.mapper.illnessesTranslateMapper(v))));
  }

  navigateToDetails(row: IIllnesses): void {
    this.router.navigateByUrl(`illnesses/${row.id}/detail`);
  }

  addNewRecord(): void {
    this.router.navigateByUrl('illnesses/new');
  }

  filter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value.toLowerCase();
    this.illnesses.filter = filterValue.trim().toLowerCase();
  }
}

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
import { Component, inject, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ApplicationConfigService } from '../../../../core';
import { IIllnesses } from '../../../../model/illnesses.interface';
import { CommentComponent, IHumanDTO } from '../../../../shared';
import { CommonCardFooterComponent } from '../../../../shared/component/common-card-footer/common-card-footer.component';
import { GenericButtonComponent } from '../../../../shared/component/generic-button/generic-button.component';
import { HelpButtonComponent } from '../../../../shared/component/help-button/help-button.component';
import { PageTitleTranslateComponent } from '../../../../shared/component/page-title-translate/page-title-translate.component';
import { DialogService } from '../../../../shared/services/dialog.service';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { IllnessConfirmComponent } from '../../helper-components/confirm/illness-confirm.component';
import { IllnessDetailedInformationComponent } from '../../helper-components/illness-detailed-information/illness-detailed-information.component';
import { IllnessService } from '../../service/illness.service';
import { downloadRecordValue } from '../../../../shared/function';
import { SharedDataService } from '../../../../shared/services/shared-data.service';

@Component({
  selector: 'vm-detail',
  standalone: true,
  imports: [
    SharedLibsModule,
    TranslateModule,
    GenericButtonComponent,
    IllnessDetailedInformationComponent,
    HelpButtonComponent,
    CommonCardFooterComponent,
    CommentComponent,
  ],
  templateUrl: './illness-detail.component.html',
  styleUrls: ['./illness-detail.component.scss'],
})
export class IllnessDetailComponent extends PageTitleTranslateComponent implements OnInit {
  get illness(): IIllnesses | null {
    return this._illness;
  }

  set illness(value: IIllnesses | null) {
    this._illness = value;
  }
  dialog = inject(DialogService);
  router: Router = inject(Router);
  illnessService = inject(IllnessService);
  appConfig = inject(ApplicationConfigService);
  matDialog = inject(MatDialog);
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  sharedDataService: SharedDataService = inject(SharedDataService);
  helpDialogTitle = 'HELP.PAST_ILLNESS.DETAIL.TITLE';
  helpDialogBody = 'HELP.PAST_ILLNESS.DETAIL.BODY';
  private _illness: IIllnesses | null = null;

  get patient(): IHumanDTO {
    return this.sharedDataService.storedData['patient']! ? this.sharedDataService.storedData['patient'] : null;
  }

  ngOnInit(): void {
    let id = this.activatedRoute.snapshot.params['id'];
    this.illnessService.find(id).subscribe(illness => {
      if (illness) {
        this.illness = illness;
      } else {
        this.illnessService.query().subscribe({
          next: list => {
            this.illness = list.find(filteredIllness => filteredIllness.id === id)!;
          },
        });
      }
    });
  }

  editRecord(illness: IIllnesses): void {
    this.router.navigate([`illnesses`, illness.id, 'edit']);
  }

  download = (illness: IIllnesses): void => downloadRecordValue<IIllnesses>(illness, this.patient);

  deleteRecord(illness: IIllnesses): void {
    /* eslint-disable-next-line security/detect-non-literal-fs-filename -- Safe as no value holds user input */
    this.matDialog
      .open(IllnessConfirmComponent, {
        width: '60vw',
        data: { value: { ...illness }, button: 'buttons.DELETE' },
      })
      .afterClosed()
      .subscribe({
        next: (confirmed: boolean) => {
          illness.confidentiality = this.baseServices.confidentialityStatus;
          if (confirmed) {
            this.illnessService.delete(illness.id!).subscribe({
              next: () => {
                window.history.back();
              },
            });
          }
        },
      });
  }
}

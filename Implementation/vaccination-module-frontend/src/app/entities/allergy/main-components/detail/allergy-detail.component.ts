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
import { IHumanDTO } from 'src/app/shared';
import { CommentComponent } from 'src/app/shared/component/comment/comment.component';
import { PageTitleTranslateComponent } from 'src/app/shared/component/page-title-translate/page-title-translate.component';
import { ApplicationConfigService } from '../../../../core';
import { Allergy } from '../../../../model/allergy.interface';
import { CommonCardFooterComponent } from '../../../../shared/component/common-card-footer/common-card-footer.component';
import { GenericButtonComponent } from '../../../../shared/component/generic-button/generic-button.component';
import { HelpButtonComponent } from '../../../../shared/component/help-button/help-button.component';
import { DialogService } from '../../../../shared/services/dialog.service';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { AllergyDetailedInformationComponent } from '../../helper-components/allergy-detailed-information/allergy-detailed-information.component';
import { AllergyConfirmComponent } from '../../helper-components/confirm/allergy-confirm.component';
import { AllergyService } from '../../services/allergy.service';
import { downloadRecordValue } from '../../../../shared/function';
import { SharedDataService } from '../../../../shared/services/shared-data.service';

@Component({
  selector: 'vm-allergy-detail',
  templateUrl: './allergy-detail.component.html',
  styleUrls: ['./allergy-detail.component.scss'],
  imports: [
    SharedLibsModule,
    TranslateModule,
    GenericButtonComponent,
    AllergyDetailedInformationComponent,
    HelpButtonComponent,
    CommonCardFooterComponent,
    CommentComponent,
  ],
  standalone: true,
})
export class AllergyDetailComponent extends PageTitleTranslateComponent implements OnInit {
  allergyService: AllergyService = inject(AllergyService);
  dialog = inject(DialogService);
  router: Router = inject(Router);
  allergy: Allergy | null = null;
  appConfig = inject(ApplicationConfigService);
  matDialog = inject(MatDialog);
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  sharedDataService: SharedDataService = inject(SharedDataService);

  helpDialogTitle = 'HELP.ALLERGY.DETAIL.TITLE';
  helpDialogBody = 'HELP.ALLERGY.DETAIL.BODY';

  get patient(): IHumanDTO {
    return this.sharedDataService.storedData['patient']! ? this.sharedDataService.storedData['patient'] : null;
  }

  ngOnInit(): void {
    let id = this.activatedRoute.snapshot.params['id'];
    this.allergyService.find(id).subscribe(allergy => {
      if (allergy) {
        this.allergy = allergy;
      } else {
        this.allergyService.query().subscribe({
          next: list => {
            this.allergy = list.find(filteredAllergy => filteredAllergy.id === id)!;
          },
        });
      }
    });
  }

  editRecord(allergy: Allergy): void {
    this.router.navigate([`allergy`, allergy.id, 'edit']);
  }

  download = (allergy: Allergy): void => downloadRecordValue<Allergy>(allergy, this.patient);

  deleteRecord(allergy: Allergy): void {
    /* eslint-disable-next-line security/detect-non-literal-fs-filename -- Safe as no value holds user input */
    this.matDialog
      .open(AllergyConfirmComponent, {
        width: '60vw',
        data: { value: { ...allergy }, button: 'buttons.DELETE' },
      })
      .afterClosed()
      .subscribe({
        next: (confirmed: boolean) => {
          if (confirmed) {
            allergy.confidentiality = this.baseServices.confidentialityStatus;
            this.spinnerService.show();
            this.allergyService.delete(allergy.id!).subscribe({
              next: () => {
                window.history.back();
              },
            });
          }
        },
      });
  }
}

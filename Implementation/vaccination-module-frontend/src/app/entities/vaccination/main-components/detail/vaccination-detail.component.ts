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
import { MatListModule } from '@angular/material/list';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { CommentComponent, IHumanDTO } from 'src/app/shared';
import { ApplicationConfigService } from '../../../../core';
import { Vaccination } from '../../../../model';
import {
  ArrayObjectsToFlatPipe,
  CommonCardFooterComponent,
  DialogService,
  FormatDatePipe,
  GenericButtonComponent,
  HelpButtonComponent,
  PageTitleTranslateComponent,
} from '../../../../shared';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { ConfirmComponent } from '../../helper-components/confirm/confirm.component';
import { VaccinationDetailedInformationComponent } from '../../helper-components/vaccination-detailed-information/vaccination-detailed-information.component';
import { VaccinationService } from '../../services/vaccination.service';
import { downloadRecordValue } from '../../../../shared/function';
import { SharedDataService } from '../../../../shared/services/shared-data.service';

@Component({
  selector: 'vm-vaccination-detail',
  templateUrl: './vaccination-detail.component.html',
  styleUrls: ['./vaccination-detail.component.scss'],
  imports: [
    SharedLibsModule,
    GenericButtonComponent,
    MatListModule,
    ArrayObjectsToFlatPipe,
    TranslateModule,
    FormatDatePipe,
    VaccinationDetailedInformationComponent,
    HelpButtonComponent,
    CommonCardFooterComponent,
    CommentComponent,
  ],
  standalone: true,
})
export class VaccinationDetailComponent extends PageTitleTranslateComponent implements OnInit {
  vaccination: Vaccination | null = null;
  vaccinationService = inject(VaccinationService);
  router: Router = inject(Router);
  dialog = inject(DialogService);
  matDialog: MatDialog = inject(MatDialog);
  appConfig: ApplicationConfigService = inject(ApplicationConfigService);
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  sharedDataService: SharedDataService = inject(SharedDataService);

  canValidate: boolean = false;
  helpDialogTitle = 'HELP.VACCINATION.DETAIL.TITLE';
  helpDialogBody = 'HELP.VACCINATION.DETAIL.BODY';

  get patient(): IHumanDTO {
    return this.sharedDataService.storedData['patient']! ? this.sharedDataService.storedData['patient'] : null;
  }

  ngOnInit(): void {
    let id = this.activatedRoute.snapshot.params['id'];
    this.vaccinationService.find(id).subscribe(vaccine => {
      if (vaccine) {
        this.vaccination = vaccine;
      } else {
        this.vaccinationService.query().subscribe({
          next: list => {
            this.vaccination = list.find(filteredVaccine => filteredVaccine.id === id)!;
          },
        });
      }
    });

    let role = this.sharedDataService.storedData['role'];
    this.canValidate = role === 'HCP' || role === 'ASS';
  }

  editRecord(vaccination: Vaccination): void {
    this.router.navigate([`vaccination`, vaccination.id, 'edit']);
  }

  deleteRecord(vaccination: Vaccination): void {
    /* eslint-disable-next-line security/detect-non-literal-fs-filename -- Safe as no value holds user input */
    this.matDialog
      .open(ConfirmComponent, {
        width: '60vw',
        data: { value: { ...vaccination }, button: 'buttons.DELETE' },
      })
      .afterClosed()
      .subscribe({
        next: (confirmed: boolean) => {
          vaccination.confidentiality = this.baseServices.confidentialityStatus;
          if (confirmed) {
            this.vaccinationService.delete(vaccination.id!).subscribe({
              next: () => {
                window.history.back();
              },
            });
          }
        },
      });
  }

  download = (vaccination: Vaccination): void => downloadRecordValue<Vaccination>(vaccination, this.patient);

  validateRecord(vaccination: Vaccination): void {
    let vaccinationCopy: Vaccination = {
      ...vaccination,
      author: {
        firstName: this.sharedDataService.storedData['ufname']!,
        lastName: this.sharedDataService.storedData['ugname']!,
        prefix: this.sharedDataService.storedData['utitle']!,
        role: this.sharedDataService.storedData['role']!,
      },
    };
    this.vaccinationService.validate(vaccinationCopy).subscribe(() => window.history.back());
  }
}

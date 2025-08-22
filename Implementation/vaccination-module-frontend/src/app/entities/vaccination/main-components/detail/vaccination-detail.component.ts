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
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommentComponent, IHumanDTO } from 'src/app/shared';
import { IVaccination } from '../../../../model';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';
import { initializeActionData } from '../../../../shared/function';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { DetailsActionComponent } from '../../../common/details-action/details-action.component';
import { VaccinationDetailedInformationComponent } from '../../helper-components/vaccination-detailed-information/vaccination-detailed-information.component';
import { VaccinationService } from '../../services/vaccination.service';

@Component({
  selector: 'vm-vaccination-detail',
  standalone: true,
  templateUrl: './vaccination-detail.component.html',
  styleUrls: ['./vaccination-detail.component.scss'],
  imports: [SharedLibsModule, VaccinationDetailedInformationComponent, DetailsActionComponent, CommentComponent],
})
export class VaccinationDetailComponent extends BreakPointSensorComponent implements OnInit {
  vaccination: IVaccination | null = null;
  vaccinationService = inject(VaccinationService);
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  sharedDataService: SharedDataService = inject(SharedDataService);

  get patient(): IHumanDTO | null {
    return this.sharedDataService.storedData['patient']! ? this.sharedDataService.storedData['patient'] : null;
  }

  ngOnInit(): void {
    this.displayMenu(true, false);
    let id = this.activatedRoute.snapshot.params['id'];
    this.vaccinationService.find(id).subscribe(vaccine => {
      if (vaccine) {
        this.vaccination = vaccine;
        this.sharedDataService.storedData['detailedItem'] = this.vaccination;
      } else {
        this.vaccinationService.query().subscribe({
          next: list => {
            this.vaccination = list.find(filteredVaccine => filteredVaccine.id === id)!;
            this.sharedDataService.storedData['detailedItem'] = this.vaccination;
            this.sharedDataService.setSessionStorage();
          },
        });
      }
    });

    initializeActionData('details', this.sharedDataService);
  }
}

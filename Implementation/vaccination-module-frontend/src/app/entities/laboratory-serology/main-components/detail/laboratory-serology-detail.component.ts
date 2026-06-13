/**
 * Copyright (c) 2026 eHealth Suisse
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
import { IHumanDTO } from 'src/app/shared';
import { SessionInfoService } from '../../../../core/security/session-info.service';
import { ILaboratorySerology } from '../../../../model';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';
import { initializeActionData } from '../../../../shared/function';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedComponentModule } from '../../../../shared/shared-component.module';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { DetailsActionComponent } from '../../../common/details-action/details-action.component';
import { LaboratorySerologyDetailedInformationComponent } from '../../helper-components/laboratory-serology-detailed-information/laboratory-serology-detailed-information.component';
import { LaboratorySerologyService } from '../../services/laboratory-serology.service';

@Component({
  selector: 'vm-laboratory-serology-detail',
  standalone: true,
  templateUrl: './laboratory-serology-detail.component.html',
  styleUrls: ['./laboratory-serology-detail.component.scss'],
  imports: [SharedLibsModule, SharedComponentModule, LaboratorySerologyDetailedInformationComponent, DetailsActionComponent],
})
export class LaboratorySerologyDetailComponent extends BreakPointSensorComponent implements OnInit {
  laboratorySerology: ILaboratorySerology | null = null;
  laboratorySerologyService: LaboratorySerologyService = inject(LaboratorySerologyService);
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);
  sharedDataService: SharedDataService = inject(SharedDataService);
  sessionInfoService: SessionInfoService = inject(SessionInfoService);

  get patient(): IHumanDTO | null {
    return this.sharedDataService.storedData['patient']! ? this.sharedDataService.storedData['patient'] : null;
  }

  ngOnInit(): void {
    this.displayMenu(true, false);
    initializeActionData('details', this.sharedDataService);
    const id = this.activatedRoute.snapshot.params['id'];
    this.laboratorySerologyService.find(id).subscribe((laboratorySerology: ILaboratorySerology) => {
      if (laboratorySerology) {
        this.laboratorySerology = laboratorySerology;
        this.sharedDataService.storedData['detailedItem'] = this.laboratorySerology;
        this.sharedDataService.setSessionStorage();
      } else {
        this.laboratorySerologyService.query().subscribe({
          next: (list: ILaboratorySerology[]) => {
            this.laboratorySerology = list.find(filteredRecord => filteredRecord.id === id)!;
            this.sharedDataService.storedData['detailedItem'] = this.laboratorySerology;
            this.sharedDataService.setSessionStorage();
          },
        });
      }
    });

    initializeActionData('details', this.sharedDataService);
  }
}

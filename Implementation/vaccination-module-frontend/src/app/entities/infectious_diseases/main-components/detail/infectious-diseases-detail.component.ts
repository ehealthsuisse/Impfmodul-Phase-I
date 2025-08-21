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
import { IInfectiousDiseases } from '../../../../model';
import { SharedLibsModule } from '../../../../shared/shared-libs.module';
import { InfectiousDiseasesDetailedInformationComponent } from '../../helper-components/infectious-diseases-detailed-information/infectious-diseases-detailed-information.component';
import { InfectiousDiseasesService } from '../../service/infectious-diseases.service';
import { SharedDataService } from '../../../../shared/services/shared-data.service';
import { SharedComponentModule } from '../../../../shared/shared-component.module';
import { DetailsActionComponent } from '../../../common/details-action/details-action.component';
import { initializeActionData } from '../../../../shared/function';
import { BreakPointSensorComponent } from '../../../../shared/component/break-point-sensor/break-point-sensor.component';

@Component({
  selector: 'vm-infectious-diseases-detail',
  standalone: true,
  imports: [SharedLibsModule, InfectiousDiseasesDetailedInformationComponent, DetailsActionComponent, SharedComponentModule],
  templateUrl: './infectious-diseases-detail.component.html',
  styleUrls: ['./infectious-diseases-detail.component.scss'],
})
export class InfectiousDiseasesDetailComponent extends BreakPointSensorComponent implements OnInit {
  infectiousDiseases: IInfectiousDiseases | null = null;
  illnessService: InfectiousDiseasesService = inject(InfectiousDiseasesService);
  activatedRoute: ActivatedRoute = inject(ActivatedRoute);

  sharedDataService: SharedDataService = inject(SharedDataService);

  ngOnInit(): void {
    this.displayMenu(true, false);
    let id = this.activatedRoute.snapshot.params['id'];
    this.illnessService.find(id).subscribe(illness => {
      if (illness) {
        this.infectiousDiseases = illness;
        this.sharedDataService.storedData['detailedItem'] = this.infectiousDiseases;
        this.sharedDataService.setSessionStorage();
      } else {
        this.illnessService.query().subscribe({
          next: list => {
            this.infectiousDiseases = list.find(filteredIllness => filteredIllness.id === id)!;
            this.sharedDataService.storedData['detailedItem'] = this.infectiousDiseases;
            this.sharedDataService.setSessionStorage();
          },
        });
      }
    });
    initializeActionData('details', this.sharedDataService);
  }
}

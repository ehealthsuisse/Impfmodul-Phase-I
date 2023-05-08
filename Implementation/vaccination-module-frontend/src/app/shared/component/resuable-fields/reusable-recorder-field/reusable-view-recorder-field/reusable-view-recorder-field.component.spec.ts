import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReusableViewRecorderFieldComponent } from './reusable-view-recorder-field.component';
import { SharedTestingModule } from '../../../../shared-testing.module';

describe('ReusableViewRecorderFieldComponent', () => {
  let component: ReusableViewRecorderFieldComponent;
  let fixture: ComponentFixture<ReusableViewRecorderFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReusableViewRecorderFieldComponent, SharedTestingModule],
    })
      .overrideTemplate(ReusableViewRecorderFieldComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ReusableViewRecorderFieldComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

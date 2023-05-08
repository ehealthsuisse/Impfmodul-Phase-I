import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReusableRecorderFieldComponent } from './reusable-recorder-field.component';
import { SharedTestingModule } from '../../../shared-testing.module';

describe('ReusableRecorderFieldComponent', () => {
  let component: ReusableRecorderFieldComponent;
  let fixture: ComponentFixture<ReusableRecorderFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReusableRecorderFieldComponent, SharedTestingModule],
    })
      .overrideTemplate(ReusableRecorderFieldComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ReusableRecorderFieldComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReusableFormRecorderFieldComponent } from './reusable-form-recorder-field.component';
import { SharedTestingModule } from '../../../../shared-testing.module';

describe('ReusableFormRecorderFieldComponent', () => {
  let component: ReusableFormRecorderFieldComponent;
  let fixture: ComponentFixture<ReusableFormRecorderFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReusableFormRecorderFieldComponent, SharedTestingModule],
    })
      .overrideTemplate(ReusableFormRecorderFieldComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ReusableFormRecorderFieldComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

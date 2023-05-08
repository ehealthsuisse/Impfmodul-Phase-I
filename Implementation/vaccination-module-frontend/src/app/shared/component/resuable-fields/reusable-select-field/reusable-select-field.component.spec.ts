import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReusableSelectFieldComponent } from './reusable-select-field.component';
import { SharedTestingModule } from '../../../shared-testing.module';

describe('ReusableSelectFieldComponent', () => {
  let component: ReusableSelectFieldComponent;
  let fixture: ComponentFixture<ReusableSelectFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReusableSelectFieldComponent, SharedTestingModule],
    })
      .overrideTemplate(ReusableSelectFieldComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ReusableSelectFieldComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

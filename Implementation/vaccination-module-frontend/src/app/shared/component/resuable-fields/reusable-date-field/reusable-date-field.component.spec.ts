import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReusableDateFieldComponent } from './reusable-date-field.component';
import { SharedTestingModule } from '../../../shared-testing.module';

describe('ReusableDateFieldComponent', () => {
  let component: ReusableDateFieldComponent;
  let fixture: ComponentFixture<ReusableDateFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReusableDateFieldComponent, SharedTestingModule],
    })
      .overrideTemplate(ReusableDateFieldComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ReusableDateFieldComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

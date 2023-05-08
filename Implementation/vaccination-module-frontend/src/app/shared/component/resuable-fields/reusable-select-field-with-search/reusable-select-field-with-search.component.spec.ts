import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReusableSelectFieldWithSearchComponent } from './reusable-select-field-with-search.component';
import { SharedTestingModule } from '../../../shared-testing.module';

describe('ReusableSelectFieldWithSearchComponent', () => {
  let component: ReusableSelectFieldWithSearchComponent;
  let fixture: ComponentFixture<ReusableSelectFieldWithSearchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReusableSelectFieldWithSearchComponent, SharedTestingModule],
    })
      .overrideTemplate(ReusableSelectFieldWithSearchComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ReusableSelectFieldWithSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

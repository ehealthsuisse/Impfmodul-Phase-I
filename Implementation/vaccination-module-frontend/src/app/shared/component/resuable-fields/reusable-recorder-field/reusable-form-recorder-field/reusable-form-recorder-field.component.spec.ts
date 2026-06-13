import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';

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
    component.parentForm = new FormGroup({
      recorder: new FormGroup({
        firstName: new FormControl(''),
        lastName: new FormControl(''),
      }),
    });
    component.isEditable = true;
    component.recorder = { id: 1, firstName: 'John', lastName: 'Doe' } as any;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should mark firstName and lastName as touched after view init', () => {
    const recorderGroup = component.parentForm.get('recorder') as FormGroup;
    expect(recorderGroup.get('firstName')?.touched).toBeTrue();
    expect(recorderGroup.get('lastName')?.touched).toBeTrue();
  });

  it('should not throw if firstName or lastName controls are missing', () => {
    component.parentForm = new FormGroup({
      recorder: new FormGroup({}),
    });
    expect(() => component.ngAfterViewInit()).not.toThrow();
  });
});

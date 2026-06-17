import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BreakPointSensorComponent } from './break-point-sensor.component';
import { BreakpointObserver, BreakpointState, Breakpoints } from '@angular/cdk/layout';
import { DialogService } from '../../services';
import { TranslateService } from '@ngx-translate/core';
import { ReplaySubject } from 'rxjs';

describe('BreakPointSensorComponent', () => {
  let component: BreakPointSensorComponent;
  let fixture: ComponentFixture<BreakPointSensorComponent>;
  let breakpointObserverSpy: jasmine.SpyObj<BreakpointObserver>;
  let dialogServiceSpy: jasmine.SpyObj<DialogService>;
  let translateServiceSpy: jasmine.SpyObj<TranslateService>;
  let breakpointSubject: ReplaySubject<BreakpointState>;

  beforeEach(async () => {
    breakpointObserverSpy = jasmine.createSpyObj('BreakpointObserver', ['observe']);
    dialogServiceSpy = jasmine.createSpyObj('DialogService', ['showActionSidenav', 'showPatientActionSidenav']);
    translateServiceSpy = jasmine.createSpyObj('TranslateService', ['instant', 'get', 'use']);

    breakpointSubject = new ReplaySubject<BreakpointState>(1);
    breakpointObserverSpy.observe.and.returnValue(breakpointSubject.asObservable());

    await TestBed.configureTestingModule({
      imports: [BreakPointSensorComponent],
      providers: [
        { provide: BreakpointObserver, useValue: breakpointObserverSpy },
        { provide: DialogService, useValue: dialogServiceSpy },
        { provide: TranslateService, useValue: translateServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BreakPointSensorComponent);
    component = fixture.componentInstance;
  });

  function emitBreakpointState(breakpoints: any): void {
    breakpointSubject.next({ breakpoints } as BreakpointState);
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should set isMobile true for mobile breakpoints', () => {
    emitBreakpointState({
      [Breakpoints.XSmall]: true,
      [Breakpoints.Small]: false,
      [Breakpoints.HandsetPortrait]: false,
      [component.MY_TABLET_BREAKPOINT]: false,
      [component.MY_DESKTOP_BREAKPOINT]: false,
      [Breakpoints.Web]: false,
    });
    expect(component.isMobile).toBeTrue();
    expect(component.isTablet).toBeFalse();
    expect(component.isDesktop).toBeFalse();
  });

  it('should set isTablet true for tablet breakpoint', () => {
    emitBreakpointState({
      [Breakpoints.XSmall]: false,
      [Breakpoints.Small]: false,
      [Breakpoints.HandsetPortrait]: false,
      [component.MY_TABLET_BREAKPOINT]: true,
      [component.MY_DESKTOP_BREAKPOINT]: false,
      [Breakpoints.Web]: false,
    });
    expect(component.isTablet).toBeTrue();
    expect(component.isMobile).toBeFalse();
    expect(component.isDesktop).toBeFalse();
  });

  it('should set isDesktop true for desktop breakpoint', () => {
    emitBreakpointState({
      [Breakpoints.XSmall]: false,
      [Breakpoints.Small]: false,
      [Breakpoints.HandsetPortrait]: false,
      [component.MY_TABLET_BREAKPOINT]: false,
      [component.MY_DESKTOP_BREAKPOINT]: true,
      [Breakpoints.Web]: false,
    });
    expect(component.isDesktop).toBeTrue();
    expect(component.isMobile).toBeFalse();
    expect(component.isTablet).toBeFalse();
  });

  it('should call dialogService methods in displayMenu', () => {
    component.displayMenu(true, false);
    expect(dialogServiceSpy.showActionSidenav).toHaveBeenCalledWith(true);
    expect(dialogServiceSpy.showPatientActionSidenav).toHaveBeenCalledWith(false);
  });
});

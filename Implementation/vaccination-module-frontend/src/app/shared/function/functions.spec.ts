import { buildComment, deleteRecord, filterPredicateExcludeJSONField, initializeActionData, parseStringToDate } from './functions';
import { SharedDataService } from '../services/shared-data.service';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';

describe('functions', () => {
  describe('parseStringToDate', () => {
    it('should parse valid date string', () => {
      expect(parseStringToDate('01.12.2023')).toEqual(new Date('2023-12-01'));
    });

    it('should return null for invalid date string', () => {
      expect(parseStringToDate('2023-12-01')).toBeNull();
      expect(parseStringToDate(null)).toBeNull();
    });
  });

  describe('filterPredicateExcludeJSONField', () => {
    it('should exclude json field and match filter', () => {
      const data = { name: 'Alice', json: '{"foo":"bar"}' };
      expect(filterPredicateExcludeJSONField(data, 'alice')).toBeTrue();
      expect(filterPredicateExcludeJSONField(data, 'bar')).toBeFalse();
    });
  });

  describe('buildComment', () => {
    it('should build comment with trimmed text', () => {
      const comment = buildComment('  hello  ');
      expect(comment).toEqual({ text: 'hello', author: 'will be added by the system' });
    });

    it('should return undefined for empty or whitespace text', () => {
      expect(buildComment('   ')).toBeUndefined();
    });

    it('should use provided author', () => {
      expect(buildComment('test', 'author')).toEqual({ text: 'test', author: 'author' });
    });
  });

  describe('deleteRecord', () => {
    let matDialogSpy: jasmine.SpyObj<MatDialog>;
    let serviceSpy: any;
    let afterClosedSpy: jasmine.Spy;
    let openSpy: jasmine.Spy;
    let details: any;

    beforeEach(() => {
      afterClosedSpy = jasmine.createSpy().and.returnValue(of(true));
      openSpy = jasmine.createSpy().and.returnValue({ afterClosed: afterClosedSpy });
      matDialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
      matDialogSpy.open = openSpy;
      serviceSpy = {
        deleteWithBody: jasmine.createSpy().and.returnValue(of({})),
      };
      details = { id: 1, confidentiality: 'conf' };
      spyOn(window.history, 'back');
    });

    it('should call deleteWithBody and go back if confirmed', () => {
      deleteRecord(matDialogSpy, serviceSpy, details, 'Delete?');
      expect(matDialogSpy.open).toHaveBeenCalled();
      expect(afterClosedSpy).toHaveBeenCalled();
      expect(serviceSpy.deleteWithBody).toHaveBeenCalledWith(1, 'conf');
      expect(window.history.back).toHaveBeenCalled();
    });

    it('should not call deleteWithBody if not confirmed', () => {
      afterClosedSpy.and.returnValue(of(false));
      deleteRecord(matDialogSpy, serviceSpy, details, 'Delete?');
      expect(serviceSpy.deleteWithBody).not.toHaveBeenCalled();
      expect(window.history.back).not.toHaveBeenCalled();
    });
  });

  describe('initializeActionData', () => {
    let sharedData: SharedDataService;

    beforeEach(() => {
      sharedData = { detailedActions: false, patientActions: false } as SharedDataService;
    });

    it('should set detailedActions true and patientActions false for "details"', () => {
      initializeActionData('details', sharedData);
      expect(sharedData.detailedActions).toBeTrue();
      expect(sharedData.patientActions).toBeFalse();
    });

    it('should set detailedActions false and patientActions true for "record"', () => {
      initializeActionData('record', sharedData);
      expect(sharedData.detailedActions).toBeFalse();
      expect(sharedData.patientActions).toBeTrue();
    });

    it('should set both false for other types', () => {
      initializeActionData('other', sharedData);
      expect(sharedData.detailedActions).toBeFalse();
      expect(sharedData.patientActions).toBeFalse();
    });
  });
});

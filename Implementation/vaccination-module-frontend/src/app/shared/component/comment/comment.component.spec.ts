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
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CommentComponent } from './comment.component';
import { TranslateModule } from '@ngx-translate/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('CommentComponent', () => {
  let component: CommentComponent;
  let fixture: ComponentFixture<CommentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommentComponent, TranslateModule.forRoot(), NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CommentComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should set commentMessage from comment on init', () => {
    component.comment = { text: 'Test comment', author: 'Author' };
    component.ngOnInit();
    expect(component.commentMessage).toBe('Test comment');
  });

  it('should set commentMessage to empty if no comment but canAddComments is true', () => {
    component.comment = null;
    component.canAddComments = true;
    component.commentMessage = 'should be cleared';
    component.ngOnInit();
    expect(component.commentMessage).toBe('');
  });

  it('should not change commentMessage if no comment and canAddComments is false', () => {
    component.comment = null;
    component.canAddComments = false;
    component.commentMessage = 'existing';
    component.ngOnInit();
    expect(component.commentMessage).toBe('existing');
  });

  it('should emit commentMessageChange on onCommentChange', () => {
    spyOn(component.commentMessageChange, 'emit');
    component.onCommentChange('new value');
    expect(component.commentMessage).toBe('new value');
    expect(component.commentMessageChange.emit).toHaveBeenCalledWith('new value');
  });
});

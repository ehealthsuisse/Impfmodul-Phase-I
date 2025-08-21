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
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { IComment } from '../../interfaces';
import { SharedLibsModule } from '../../shared-libs.module';

@Component({
  selector: 'vm-comment',
  standalone: true,
  imports: [SharedLibsModule],
  templateUrl: './comment.component.html',
  styleUrls: ['./comment.component.scss'],
})
export class CommentComponent implements OnInit {
  @Input() comment: IComment | null = null;
  @Input() canAddComments: boolean = false;
  @Input() commentMessage: string = '';
  @Output() commentMessageChange = new EventEmitter<string>();

  ngOnInit(): void {
    if (this.comment) {
      this.commentMessage = this.comment.text || '';
    } else if (this.canAddComments) {
      this.commentMessage = '';
    }
  }

  onCommentChange(value: string): void {
    this.commentMessage = value;
    this.commentMessageChange.emit(value);
  }
}

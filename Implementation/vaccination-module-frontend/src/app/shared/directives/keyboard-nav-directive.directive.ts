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
import { Directive, ElementRef, HostListener, inject, Input } from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';

@Directive({
  selector: '[vmKeyboardNavDirective]',
  standalone: true,
})
export class KeyboardNavDirectiveDirective {
  @Input() trigger!: MatMenuTrigger;
  el: ElementRef = inject(ElementRef);

  private navLinks: ElementRef[] = this.el.nativeElement.querySelectorAll('a');
  private selectedIndex = 0;

  @HostListener('keydown', ['$event']) onKeydown(event: KeyboardEvent): void {
    switch (event.key) {
      case 'ArrowDown':
        this.selectNextItem();
        break;
      case 'ArrowUp':
        this.selectPrevItem();
        break;
      case 'Enter':
        this.invokeSelectedItem();
        break;
      case 'Escape':
        this.closeMenu();
        break;
    }
  }

  private selectNextItem(): void {
    console.error(this.selectedIndex, this.navLinks.length);
    if (this.selectedIndex < this.navLinks.length - 1) {
      this.selectedIndex++;
      this.navLinks[this.selectedIndex].nativeElement.focus();
    }
  }

  private selectPrevItem(): void {
    if (this.selectedIndex > 0) {
      this.selectedIndex--;
      this.navLinks[this.selectedIndex].nativeElement.focus();
    }
  }

  private invokeSelectedItem(): void {
    this.navLinks[this.selectedIndex].nativeElement.click();
    this.closeMenu();
  }

  private closeMenu(): void {
    // Close the menu and return focus to the menubar
    this.trigger.closeMenu();
  }
}

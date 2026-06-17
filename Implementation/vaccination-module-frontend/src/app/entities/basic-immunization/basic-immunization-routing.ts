/**
 * Copyright (c) 2023 eHealth Suisse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import { Routes } from '@angular/router';

export const BASIC_IMMUNIZATION_ROUTE: Routes = [
  {
    path: '',
    loadComponent: () => import('./main-components').then(c => c.BasicImmunizationListComponent),
    title: 'BASIC_IMMUNIZATION.TITLE',
    data: {
      type: 'basic-immunization',
    },
  },
  {
    path: ':id/detail',
    loadComponent: () => import('./main-components').then(c => c.BasicImmunizationDetailComponent),
    title: 'BASIC_IMMUNIZATION.DETAILS_HEADER',
    data: {
      type: 'basic-immunization',
    },
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./main-components').then(c => c.BasicImmunizationFormComponent),
    title: 'BASIC_IMMUNIZATION.EDIT_HEADER',
    data: {
      type: 'basic-immunization',
    },
  },
  {
    path: 'new',
    loadComponent: () => import('./main-components').then(c => c.BasicImmunizationFormComponent),
    title: 'BASIC_IMMUNIZATION.NEW_HEADER',
    data: {
      type: 'basic-immunization',
    },
  },
];

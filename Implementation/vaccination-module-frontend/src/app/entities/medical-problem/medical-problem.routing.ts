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
import { Routes } from '@angular/router';

export const PROBLEM_ROUTE: Routes = [
  {
    path: '',
    loadComponent: () => import('./main-components').then(c => c.MedicalProblemListComponent),
    title: 'MEDICAL_PROBLEM.TITLE',
    data: {
      type: 'problem',
    },
  },
  {
    path: ':id/detail',
    loadComponent: () => import('./main-components').then(c => c.MedicalProblemDetailComponent),
    title: 'MEDICAL_PROBLEM.DETAILS_HEADER',
    data: {
      type: 'problem',
    },
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./main-components').then(c => c.MedicalProblemFormComponent),
    title: 'MEDICAL_PROBLEM.EDIT_HEADER',
    data: {
      type: 'problem',
    },
  },
  {
    path: 'new',
    loadComponent: () => import('./main-components').then(c => c.MedicalProblemFormComponent),
    title: 'MEDICAL_PROBLEM.NEW_HEADER',
    data: {
      type: 'problem',
    },
  },
];

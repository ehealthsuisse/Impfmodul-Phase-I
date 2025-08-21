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
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SAMLAssertionRoute } from './core/security/samlassertion-consumer/samlassertion-route';
import { VACCINATION_RECORD_ROUTING } from './entities/vaccination-record/vaccination-record-routing';
import { ERROR_ROUTE } from './layouts/error/error.route';
import { NAVBAR_ROUTING } from './layouts/navbar/navbar-routing';

const routes: Routes = [
  NAVBAR_ROUTING,
  VACCINATION_RECORD_ROUTING,
  SAMLAssertionRoute,
  {
    path: '',
    loadChildren: () => import('./entities/entities-routing').then(r => r.ENTITIES_ROUTE),
  },
  ...ERROR_ROUTE,
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}

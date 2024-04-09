import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpXsrfTokenExtractor } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class CsrfInterceptor implements HttpInterceptor {
  constructor(private tokenExtractor: HttpXsrfTokenExtractor) {}
  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const headerName = 'X-XSRF-TOKEN';
    const csrfToken = this.tokenExtractor.getToken() as string;
    let modifiedRequest = request;

    if (csrfToken && !request.headers.has(headerName)) {
      modifiedRequest = request.clone({
        headers: request.headers.set(headerName, csrfToken),
        withCredentials: true,
      });

      return next.handle(modifiedRequest);
    } else {
      modifiedRequest = request.clone({
        headers: request.headers,
        withCredentials: true,
      });
    }
    
    return next.handle(modifiedRequest);
  }
}
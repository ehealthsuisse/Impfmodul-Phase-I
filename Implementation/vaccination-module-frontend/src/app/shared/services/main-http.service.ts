import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, tap } from 'rxjs';
import { ConfigService } from '../../core/config/config.service';
import { SessionInfoService } from '../../core/security/session-info.service';
import { IBaseDTO, IValueDTO } from '../interfaces';
import { SpinnerService } from './spinner.service';

@Injectable({
  providedIn: 'root',
})
export abstract class MainHttpService<T extends IBaseDTO> {
  records: T[] = [];
  resource: string = '';
  abstract prefix: string;

  constructor(
    private configService: ConfigService,
    private http: HttpClient,
    private sessionInfoService: SessionInfoService,
    private spinnerService: SpinnerService
  ) {
    this.resource = `communityIdentifier/${this.configService.communityId}/oid/${
      this.sessionInfoService.queryParams.laaoid || this.configService.defaultLaaoid
    }/localId/${this.sessionInfoService.queryParams.lpid || this.configService.defaultLpid}`;
  }

  query(): Observable<T[]> {
    this.spinnerService.show();
    return this.http
      .get<T[]>(`${this.configService.endpointPrefix}/${this.prefix}/${this.resource}`, { params: new HttpParams() })
      .pipe(tap(records => (this.records = records)))
      .pipe(tap(() => this.spinnerService.hide()));
  }

  create(entity: T): Observable<T> {
    this.spinnerService.show();
    let t = { author: this.sessionInfoService.author.getValue(), ...entity };
    return this.http
      .post<T>(`${this.configService.endpointPrefix}/${this.prefix}/${this.resource}`, t)
      .pipe(tap(() => this.spinnerService.hide()));
  }

  update(entity: T): Observable<T> {
    this.spinnerService.show();
    return this.http
      .post<T>(`${this.configService.endpointPrefix}/${this.prefix}/${this.resource}/uuid/${entity.id}`, entity)
      .pipe(tap(() => this.spinnerService.hide()));
  }

  deleteWithBody(id: string, body: IValueDTO): any {
    this.spinnerService.show();
    return this.http
      .delete(`${this.configService.endpointPrefix}/${this.prefix}/${this.resource}/uuid/${id}`, { body })
      .pipe(tap(() => this.spinnerService.hide()));
  }

  find(id?: string): Observable<T> {
    this.spinnerService.show();
    return of(this.records.find(e => e.id === id)!).pipe(tap(() => this.spinnerService.hide()));
  }

  validate(entity: T): Observable<T> {
    let t = {
      ...entity,
    };
    return this.http
      .post<T>(`${this.configService.endpointPrefix}/${this.prefix}/validate/${this.resource}/uuid/${t.id}`, t)
      .pipe(tap(() => this.spinnerService.hide()));
  }
}
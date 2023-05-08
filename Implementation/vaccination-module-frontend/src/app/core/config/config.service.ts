import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ConfigService {
  http: HttpClient = inject(HttpClient);
  private _endpointPrefix!: string;
  private _communityId!: string;
  private _defaultLaaoid: string = '1.3.6.1.4.1.21367.13.20.3000';
  private _defaultLpid: string = 'CHPAM4489';
  private _canActivate: boolean = false;

  get communityId(): string {
    return this._communityId;
  }

  set communityId(value: string) {
    this._communityId = value;
  }

  get canActivate(): boolean {
    return this._canActivate;
  }

  set canActivate(value: boolean) {
    this._canActivate = value;
  }

  public get defaultLaaoid(): string {
    return this._defaultLaaoid;
  }

  public get defaultLpid(): string {
    return this._defaultLpid;
  }

  get endpointPrefix(): string {
    return this._endpointPrefix;
  }

  set endpointPrefix(value: string) {
    this._endpointPrefix = value;
  }

  async initialize(): Promise<void> {
    await this.loadConfig();
  }

  private async loadConfig(): Promise<void> {
    let configUrl = 'assets/config/production-config.json';

    if (process.env['NODE_ENV'] === 'development') {
      configUrl = 'assets/config/development-config.json';
    }

    const config = await this.http.get<any>(configUrl).toPromise();

    this.endpointPrefix = config.backendURL;
    this.communityId = config.communityId;
    this.canActivate = config.allowStartWithoutInitialCall;
  }
}

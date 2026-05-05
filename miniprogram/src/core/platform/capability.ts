export interface ScanOpts { onlyFromCamera?: boolean; scanType?: ('qrCode' | 'barCode' | 'datamatrix' | 'pdf417')[] }
export interface ScanResult { code: string; rawType: string }
export interface LocationOpts { type?: 'wgs84' | 'gcj02' }
export interface Location { latitude: number; longitude: number; accuracy: number }
export interface PhotoOpts { count?: number; sourceType?: ('album' | 'camera')[]; sizeType?: ('original' | 'compressed')[] }
export interface LocalFile { path: string; size: number }
export interface UploadOpts { url: string; name: string; formData?: Record<string, string>; header?: Record<string, string> }
export interface RemoteFile { url: string; key: string }
export interface SubscribeResult { [templateId: string]: 'accept' | 'reject' | 'ban' | 'filter' }
export interface SystemInfo { platform: string; sdkVersion: string }

export interface KVStorage {
  get<T = unknown>(key: string): T | undefined
  set(key: string, value: unknown): void
  remove(key: string): void
}

export type PlatformId = 'mp-weixin' | 'mp-alipay' | 'mp-toutiao' | 'h5' | 'app'

export interface PlatformCapability {
  scan(opts?: ScanOpts): Promise<ScanResult>
  getLocation(opts?: LocationOpts): Promise<Location>
  takePhoto(opts: PhotoOpts): Promise<LocalFile[]>
  uploadFile(file: LocalFile, opts: UploadOpts): Promise<RemoteFile>
  requestSubscribeMessage(templateIds: string[]): Promise<SubscribeResult>
  storage: KVStorage
  systemInfo(): SystemInfo
  readonly platform: PlatformId
}

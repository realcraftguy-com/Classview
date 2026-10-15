import type {
  ClassSnapshot,
  CodeResponse,
  CreateClassRequest,
  CreateClassResponse,
  ErrorResponse,
  RecoverResponse,
  TeacherDto,
  UpdateClassRequest,
} from './types';

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

async function request<T>(method: string, path: string, opts: { token?: string; body?: unknown } = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (opts.token) headers.Authorization = `Bearer ${opts.token}`;
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
  let res: Response;
  try {
    res = await fetch(path, {
      method,
      headers,
      body: opts.body === undefined ? undefined : JSON.stringify(opts.body),
      cache: 'no-store',
    });
  } catch {
    throw new ApiError(0, 'network', 'Keine Verbindung zum Server. Ist der Klassenserver eingeschaltet?');
  }
  if (!res.ok) {
    let err: ErrorResponse = { error: 'http_' + res.status, message: `Fehler ${res.status}` };
    try {
      err = await res.json();
    } catch {
      /* kein JSON */
    }
    throw new ApiError(res.status, err.error, err.message);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

/** Grobe Beschreibung dieses Geräts für die Liste „Angemeldete Lehrer-Geräte“. */
export function deviceLabel(): string {
  const ua = navigator.userAgent;
  const touchMac = /Macintosh/.test(ua) && navigator.maxTouchPoints > 1;
  const device = /iPad/.test(ua) || touchMac ? 'iPad' : /iPhone/.test(ua) ? 'iPhone' : /Macintosh/.test(ua) ? 'Mac' : /Android/.test(ua) ? 'Android' : /Windows/.test(ua) ? 'Windows' : 'Browser';
  const standalone = window.matchMedia('(display-mode: standalone)').matches;
  return `${device} · ${standalone ? 'Home-Bildschirm' : 'Safari'}`;
}

export const api = {
  createClass: (body: CreateClassRequest) => request<CreateClassResponse>('POST', '/api/classes', { body }),
  recover: (recoveryToken: string) =>
    request<RecoverResponse>('POST', '/api/classes/recover', { body: { recoveryToken, deviceLabel: deviceLabel() } }),
  snapshot: (id: string, token: string) => request<ClassSnapshot>('GET', `/api/classes/${id}`, { token }),
  update: (id: string, token: string, body: UpdateClassRequest) =>
    request<ClassSnapshot>('PATCH', `/api/classes/${id}`, { token, body }),
  rotateCode: (id: string, token: string) => request<CodeResponse>('POST', `/api/classes/${id}/code`, { token }),
  rotateRecovery: (id: string, token: string) =>
    request<{ recoveryToken: string }>('POST', `/api/classes/${id}/recovery`, { token }),
  teachers: (id: string, token: string) => request<TeacherDto[]>('GET', `/api/classes/${id}/teachers`, { token }),
  revokeTeacher: (id: string, token: string, teacherId: string) =>
    request<void>('DELETE', `/api/classes/${id}/teachers/${teacherId}`, { token }),
  removeDevice: (id: string, token: string, deviceId: string) =>
    request<void>('DELETE', `/api/classes/${id}/devices/${deviceId}`, { token }),
  deleteClass: (id: string, token: string) => request<void>('DELETE', `/api/classes/${id}`, { token }),

  /** Profilbild als Blob – nur mit Lehrer-Token, deshalb fetch statt <img src>. */
  async avatar(id: string, token: string, deviceId: string): Promise<Blob> {
    const res = await fetch(`/api/classes/${id}/devices/${deviceId}/avatar`, { headers: { Authorization: `Bearer ${token}` } });
    if (!res.ok) throw new ApiError(res.status, 'avatar', 'Profilbild nicht verfügbar');
    return res.blob();
  },
};

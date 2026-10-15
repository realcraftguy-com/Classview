// Spiegel von backend/protocol/.../Api.kt. Zeitstempel sind Unix-Millisekunden.

export type CodeTtl = 'NEVER' | 'HOUR' | 'DAY' | 'WEEK';

export interface ErrorResponse {
  error: string;
  message: string;
}

export interface CreateClassRequest {
  name: string;
  teacherName?: string;
  codeTtl: CodeTtl;
  inviteKey: string;
  deviceLabel?: string;
}

export interface CreateClassResponse {
  classId: string;
  name: string;
  code: string;
  codeExpiresAt: number | null;
  teacherToken: string;
  recoveryToken: string;
}

export interface RecoverResponse {
  classId: string;
  name: string;
  teacherToken: string;
}

export interface UpdateClassRequest {
  name?: string;
  teacherName?: string;
  codeTtl?: CodeTtl;
  joinPaused?: boolean;
}

export interface CodeResponse {
  code: string;
  codeExpiresAt: number | null;
}

export interface DeviceDto {
  id: string;
  displayName: string;
  avatarPreset: string | null;
  hasImage: boolean;
  avatarVersion: number;
  online: boolean;
  lastSeenAt: number | null;
  joinedAt: number;
}

export interface ClassSnapshot {
  id: string;
  name: string;
  teacherName: string | null;
  code: string | null;
  codeExpiresAt: number | null;
  codeTtl: CodeTtl;
  joinPaused: boolean;
  maxDevices: number;
  devices: DeviceDto[];
}

export interface TeacherDto {
  id: string;
  label: string;
  createdAt: number;
  lastUsedAt: number | null;
  current: boolean;
}

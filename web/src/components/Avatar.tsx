import { useEffect, useId, useState, type ReactElement } from 'react';
import { api } from '../api/client';
import type { DeviceDto } from '../api/types';

// Eingebaute Avatare – dieselben Formen wie android/core-ui/.../Avatar.kt (100×100-Raster).
type Preset = { bg: string; fg: string; draw: (fg: string, id: string) => ReactElement };

const star = Array.from({ length: 10 }, (_, i) => {
  const r = i % 2 ? 12 : 29;
  const a = -Math.PI / 2 + (i * Math.PI) / 5;
  return `${i ? 'L' : 'M'}${(50 + r * Math.cos(a)).toFixed(1)} ${(52 + r * Math.sin(a)).toFixed(1)}`;
}).join('') + 'Z';

const PRESETS: Record<string, Preset> = {
  welle: { bg: '#dce8fd', fg: '#2b6be4', draw: (fg) => (<>
    <path d="M18 42C28 32 38 32 50 42S72 52 82 42" fill="none" stroke={fg} strokeWidth="7" strokeLinecap="round" />
    <path d="M18 60C28 50 38 50 50 60S72 70 82 60" fill="none" stroke={fg} strokeOpacity=".55" strokeWidth="7" strokeLinecap="round" />
  </>) },
  berg: { bg: '#e1f0e6', fg: '#2f7a4d', draw: (fg) => (<>
    <path d="M14 72 40 34 66 72Z" fill={fg} />
    <path d="M46 72 64 46 86 72Z" fill={fg} fillOpacity=".6" />
    <path d="M33 44 40 34 47 44Z" fill="#fff" fillOpacity=".85" />
  </>) },
  sonne: { bg: '#fff0d4', fg: '#db8f12', draw: (fg) => (<>
    <circle cx="50" cy="50" r="15" fill={fg} />
    {Array.from({ length: 8 }, (_, i) => {
      const a = (i * Math.PI) / 4;
      return <line key={i} x1={50 + 23 * Math.cos(a)} y1={50 + 23 * Math.sin(a)} x2={50 + 31 * Math.cos(a)} y2={50 + 31 * Math.sin(a)} stroke={fg} strokeWidth="6" strokeLinecap="round" />;
    })}
  </>) },
  mond: { bg: '#e8e5f9', fg: '#5b4fc4', draw: (fg, id) => (<>
    <mask id={id}><rect width="100" height="100" fill="#fff" /><circle cx="62" cy="42" r="21" fill="#000" /></mask>
    <circle cx="50" cy="50" r="24" fill={fg} mask={`url(#${id})`} />
  </>) },
  stern: { bg: '#fce6eb', fg: '#cb4468', draw: (fg) => <path d={star} fill={fg} /> },
  blatt: { bg: '#e2f3ee', fg: '#248a6e', draw: (fg) => (<>
    <path d="M26 74C24 40 46 22 78 24 80 56 60 76 26 74Z" fill={fg} />
    <line x1="30" y1="70" x2="66" y2="36" stroke="#fff" strokeOpacity=".75" strokeWidth="3.5" strokeLinecap="round" />
  </>) },
  wolke: { bg: '#e3f0f7', fg: '#3580a3', draw: (fg) => (<>
    <circle cx="37" cy="55" r="13" fill={fg} /><circle cx="53" cy="47" r="17" fill={fg} />
    <circle cx="67" cy="56" r="12" fill={fg} /><rect x="37" y="55" width="30" height="13" fill={fg} />
  </>) },
  planet: { bg: '#f4e7da', fg: '#ae6430', draw: (fg) => (<>
    <circle cx="50" cy="50" r="17" fill={fg} />
    <ellipse cx="50" cy="50" rx="30" ry="8" fill="none" stroke={fg} strokeOpacity=".7" strokeWidth="4.5" />
    <path d="M33 50a17 17 0 0 1 34 0Z" fill={fg} />
  </>) },
};

export function PresetAvatar({ preset, name }: { preset: string | null; name: string }) {
  const id = useId().replace(/:/g, '');
  const p = preset ? PRESETS[preset] : undefined;
  if (!p) {
    return (
      <svg viewBox="0 0 100 100" aria-hidden="true">
        <rect width="100" height="100" fill="var(--surface-muted)" />
        <text x="50" y="50" dy=".35em" textAnchor="middle" fontSize="42" fontWeight="600" fill="var(--text-2)">
          {name.trim().charAt(0).toUpperCase()}
        </text>
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 100 100" aria-hidden="true">
      <rect width="100" height="100" fill={p.bg} />
      {p.draw(p.fg, 'm' + id)}
    </svg>
  );
}

// Profilbilder: einmal pro Gerät und Version laden, als Blob-URL zwischenspeichern.
const imageCache = new Map<string, Promise<string>>();

function loadImage(classId: string, token: string, d: DeviceDto): Promise<string> {
  const key = `${d.id}:${d.avatarVersion}`;
  let p = imageCache.get(key);
  if (!p) {
    p = api.avatar(classId, token, d.id).then((b) => URL.createObjectURL(b));
    p.catch(() => imageCache.delete(key));
    imageCache.set(key, p);
  }
  return p;
}

export function DeviceAvatar({ classId, token, device, size }: { classId: string; token: string; device: DeviceDto; size: number }) {
  const [src, setSrc] = useState<string | null>(null);
  useEffect(() => {
    let live = true;
    setSrc(null);
    if (device.hasImage) loadImage(classId, token, device).then((u) => live && setSrc(u), () => {});
    return () => {
      live = false;
    };
  }, [classId, token, device.id, device.avatarVersion, device.hasImage]);
  return (
    <span className="avatar" style={{ width: size, height: size }}>
      {src ? <img src={src} alt="" /> : <PresetAvatar preset={device.avatarPreset} name={device.displayName} />}
    </span>
  );
}

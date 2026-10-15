import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError, api } from '../api/client';
import type { ClassSnapshot, DeviceDto } from '../api/types';
import { DeviceAvatar } from '../components/Avatar';
import { BackButton, CodeText, Sheet, Spinner, Toast, copyText, formatWhen, useFlash } from '../components/ui';
import { go } from '../router';
import { store, type SavedClass } from '../store';

/** TODO: Phase 5 – auf true setzen, sobald der Server den Online-Status über WebSockets kennt. */
export const LIVE_STATUS = false;

/**
 * Lädt den Stand einer Klasse. TODO: Phase 5 – statt alle 5 s neu zu laden, kommen Änderungen live über
 * den WebSocket (class.snapshot / device.*).
 */
export function useClass(saved: SavedClass) {
  const [snap, setSnap] = useState<ClassSnapshot | null>(null);
  const [error, setError] = useState<ApiError | null>(null);

  const reload = useCallback(async () => {
    try {
      const s = await api.snapshot(saved.id, saved.teacherToken);
      setSnap(s);
      setError(null);
      store.renameClass(saved.id, s.name);
    } catch (e) {
      setError(e instanceof ApiError ? e : new ApiError(0, 'unknown', 'Unbekannter Fehler'));
    }
  }, [saved.id, saved.teacherToken]);

  useEffect(() => {
    reload();
    const t = setInterval(() => document.visibilityState === 'visible' && reload(), 5000);
    const onVisible = () => document.visibilityState === 'visible' && reload();
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      clearInterval(t);
      document.removeEventListener('visibilitychange', onVisible);
    };
  }, [reload]);

  return { snap, setSnap, error, reload };
}

export function NoAccess({ saved, error }: { saved: SavedClass; error: ApiError }) {
  const lost = error.status === 401 || error.status === 403 || error.status === 404;
  return (
    <div className="wrap">
      <div className="navbar"><BackButton label="Klassen" onClick={() => go('/')} /></div>
      <div className="empty card">
        <h2>{lost ? 'Kein Zugriff mehr' : 'Server nicht erreichbar'}</h2>
        <p>
          {lost
            ? 'Diese Klasse wurde gelöscht oder dieses Gerät wurde abgemeldet. Mit dem Wiederherstellungslink kannst du sie wieder öffnen.'
            : error.message}
        </p>
        {lost && (
          <button className="btn btn-danger" type="button" onClick={() => { store.removeClass(saved.id); go('/'); }}>
            Aus der Liste entfernen
          </button>
        )}
      </div>
    </div>
  );
}

function StudentSheet({ saved, device, onClose, onRemoved }: { saved: SavedClass; device: DeviceDto; onClose: () => void; onRemoved: () => void }) {
  const [confirm, setConfirm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function remove() {
    setBusy(true);
    try {
      await api.removeDevice(saved.id, saved.teacherToken, device.id);
      onRemoved();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Fehler');
      setBusy(false);
    }
  }

  return (
    <Sheet title={device.displayName} onClose={onClose}>
      <div className="student-detail">
        <DeviceAvatar classId={saved.id} token={saved.teacherToken} device={device} size={112} />
        <h2>{device.displayName}</h2>
        {LIVE_STATUS && (
          <p className="subtitle">
            <span className={`dot ${device.online ? 'on' : 'off'}`} />&nbsp; {device.online ? 'Online' : 'Offline'}
          </p>
        )}
      </div>
      <div className="info-list card">
        <div className="info-row"><span>Beigetreten</span><span>{formatWhen(device.joinedAt)}</span></div>
        <div className="info-row"><span>Zuletzt gesehen</span><span>{device.lastSeenAt ? formatWhen(device.lastSeenAt) : '–'}</span></div>
      </div>
      <button className="btn btn-primary" type="button" disabled>Bildschirm ansehen</button>
      <p className="hint center">Bildschirm ansehen kommt in Phase 6.</p>
      {error && <p className="error-text">{error}</p>}
      {confirm ? (
        <div className="confirm">
          <p>{device.displayName} wird aus der Klasse entfernt und muss neu beitreten.</p>
          <div className="row-gap">
            <button className="btn btn-tonal" type="button" onClick={() => setConfirm(false)}>Abbrechen</button>
            <button className="btn btn-danger" type="button" disabled={busy} onClick={remove}>Entfernen</button>
          </div>
        </div>
      ) : (
        <button className="btn btn-danger" type="button" onClick={() => setConfirm(true)}>Aus der Klasse entfernen</button>
      )}
    </Sheet>
  );
}

export function ClassView({ saved }: { saved: SavedClass }) {
  const { snap, setSnap, error, reload } = useClass(saved);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<string | null>(null);
  const [rotating, setRotating] = useState(false);
  const [confirmRotate, setConfirmRotate] = useState(false);
  const [flash, setFlash] = useFlash();

  const duplicates = useMemo(() => {
    const counts = new Map<string, number>();
    snap?.devices.forEach((d) => counts.set(d.displayName.toLowerCase(), (counts.get(d.displayName.toLowerCase()) ?? 0) + 1));
    return counts;
  }, [snap]);

  if (error && (!snap || error.status === 401 || error.status === 403 || error.status === 404)) return <NoAccess saved={saved} error={error} />;
  if (!snap) return <div className="wrap center-page"><Spinner /></div>;

  const devices = snap.devices.filter((d) => d.displayName.toLowerCase().includes(query.trim().toLowerCase()));
  const online = snap.devices.filter((d) => d.online).length;
  const selectedDevice = snap.devices.find((d) => d.id === selected);
  const newest = Math.max(0, ...snap.devices.map((d) => d.joinedAt));

  async function rotate() {
    setRotating(true);
    try {
      const r = await api.rotateCode(saved.id, saved.teacherToken);
      setSnap({ ...snap!, code: r.code, codeExpiresAt: r.codeExpiresAt });
      setFlash('Neuer Code erstellt');
    } catch (e) {
      setFlash(e instanceof ApiError ? e.message : 'Fehler');
    }
    setRotating(false);
    setConfirmRotate(false);
  }

  return (
    <div className="wrap">
      <div className="navbar">
        <BackButton label="Klassen" onClick={() => go('/')} />
        <div className="grow" />
        <button className="btn btn-plain" type="button" onClick={() => go(`/class/${saved.id}/settings`)}>Einstellungen</button>
      </div>

      {error && <div className="banner"><Spinner /> Verbindung zum Server unterbrochen – wird erneut versucht …</div>}

      <div className="class-head">
        <div>
          <h1 className="large-title">{snap.name}</h1>
          {snap.teacherName && <p className="subtitle">{snap.teacherName}</p>}
        </div>
        <div className="card code-box">
          <div>
            <div className="eyebrow">Code</div>
            <div className="code"><CodeText code={snap.code} /></div>
            <div className="code-meta">
              {snap.joinPaused ? 'Beitritt pausiert' : snap.codeExpiresAt ? `läuft ab ${formatWhen(snap.codeExpiresAt)}` : 'läuft nicht ab'}
            </div>
          </div>
          <div className="code-actions">
            <button className="btn btn-tonal" type="button" disabled={!snap.code}
              onClick={async () => setFlash((await copyText(snap.code ?? '')) ? 'Code kopiert' : 'Kopieren nicht möglich')}>Kopieren</button>
            {confirmRotate ? (
              <>
                <button className="btn btn-danger" type="button" disabled={rotating} onClick={rotate}>Alten Code ungültig machen</button>
                <button className="btn btn-plain" type="button" onClick={() => setConfirmRotate(false)}>Abbrechen</button>
              </>
            ) : (
              <button className="btn btn-tonal" type="button" onClick={() => setConfirmRotate(true)}>Erneuern</button>
            )}
            <button className="btn btn-primary" type="button" onClick={() => go(`/class/${saved.id}/code`)}>Schüler hinzufügen</button>
          </div>
        </div>
      </div>

      <div className="toolbar">
        <div className="count">
          {LIVE_STATUS && <span className={`dot ${online ? 'on' : 'off'}`} />}
          {snap.devices.length} Schüler {LIVE_STATUS && <span>· {online} online</span>}
        </div>
        <label className="search">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
          <input className="input" id="search" placeholder="Schüler suchen" aria-label="Schüler suchen" value={query} onChange={(e) => setQuery(e.target.value)} />
        </label>
      </div>

      {snap.devices.length === 0 ? (
        <div className="empty card">
          <h2>Noch niemand beigetreten</h2>
          <p>Tippe auf „Schüler hinzufügen“ und zeige den Code. Neue Schüler erscheinen hier von selbst.</p>
        </div>
      ) : (
        <div className="student-grid">
          {devices.map((d) => {
            const dup = (duplicates.get(d.displayName.toLowerCase()) ?? 0) > 1;
            const isNew = d.joinedAt === newest && Date.now() - d.joinedAt < 10 * 60_000;
            return (
              <button key={d.id} type="button" className={`student ${LIVE_STATUS && !d.online ? 'offline' : ''}`} onClick={() => setSelected(d.id)}>
                {dup ? <span className="flag">Name doppelt</span> : isNew ? <span className="flag new">Neu</span> : null}
                <DeviceAvatar classId={saved.id} token={saved.teacherToken} device={d} size={76} />
                <span className="name">{d.displayName}</span>
                <span className="status">
                  {LIVE_STATUS ? <><span className={`dot ${d.online ? 'on' : 'off'}`} />{d.online ? 'Online' : 'Offline'}</> : `seit ${formatWhen(d.joinedAt).replace('heute, ', '')}`}
                </span>
              </button>
            );
          })}
        </div>
      )}
      <p className="hint phase-note">Online-Status und sofortige Aktualisierung kommen in Phase 5. Bis dahin lädt die Liste alle 5 Sekunden neu.</p>

      {selectedDevice && (
        <StudentSheet saved={saved} device={selectedDevice} onClose={() => setSelected(null)}
          onRemoved={() => { setSelected(null); reload(); setFlash('Schüler entfernt'); }} />
      )}
      <Toast message={flash} />
    </div>
  );
}

export function CodeFullscreen({ saved }: { saved: SavedClass }) {
  const { snap, error } = useClass(saved);
  if (error && !snap) return <NoAccess saved={saved} error={error} />;
  return (
    <div className="fullcode">
      <button className="btn btn-tonal close" type="button" onClick={() => go(`/class/${saved.id}`)}>Fertig</button>
      {snap ? (
        <div>
          <div className="eyebrow">{snap.name}</div>
          <div className="big"><CodeText code={snap.code} /></div>
          <p className="steps">Öffne ClassroomView auf deinem Tablet, tippe auf „Klasse beitreten“ und gib diesen Code ein.</p>
          <p className="server-host">Server-Adresse (nur beim ersten Mal): <strong>{location.host}</strong></p>
          {snap.joinPaused && <p className="error-text">Der Beitritt ist gerade pausiert.</p>}
          <div className="live-count"><span className="dot on" /> {snap.devices.length} Schüler beigetreten</div>
        </div>
      ) : <Spinner />}
    </div>
  );
}

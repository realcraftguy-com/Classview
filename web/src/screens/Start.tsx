import { useEffect, useState } from 'react';
import { ApiError, api, deviceLabel } from '../api/client';
import type { ClassSnapshot, CodeTtl } from '../api/types';
import { PresetAvatar } from '../components/Avatar';
import { CodeText, Segmented, Sheet, TTL_OPTIONS } from '../components/ui';
import { go } from '../router';
import { store, useLocalState, type SavedClass } from '../store';

type Info = { snap?: ClassSnapshot; error?: string };

function ClassCard({ c, info }: { c: SavedClass; info?: Info }) {
  const snap = info?.snap;
  const online = snap?.devices.filter((d) => d.online).length ?? 0;
  return (
    <button className="card class-card" type="button" onClick={() => go(`/class/${c.id}`)}>
      <h3>{snap?.name ?? c.name}</h3>
      {snap && snap.devices.length > 0 && (
        <div className="faces">
          {snap.devices.slice(0, 6).map((d) => (
            <span className="avatar" key={d.id}><PresetAvatar preset={d.avatarPreset} name={d.displayName} /></span>
          ))}
        </div>
      )}
      <div className="meta">
        {info?.error ? (
          <span className="error-text">{info.error}</span>
        ) : snap ? (
          <>
            <span>{snap.devices.length} Schüler{online ? ` · ${online} online` : ''}</span>
            <span>Code <span className="code-mini"><CodeText code={snap.code} /></span></span>
          </>
        ) : (
          <span>lädt …</span>
        )}
      </div>
    </button>
  );
}

function CreateSheet({ inviteKey, onClose }: { inviteKey: string; onClose: () => void }) {
  const [name, setName] = useState('');
  const [teacher, setTeacher] = useState('');
  const [ttl, setTtl] = useState<CodeTtl>('DAY');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function create() {
    setBusy(true);
    setError(null);
    try {
      const res = await api.createClass({ name, teacherName: teacher || undefined, codeTtl: ttl, inviteKey, deviceLabel: deviceLabel() });
      store.upsertClass({ id: res.classId, name: res.name, teacherToken: res.teacherToken, recoveryToken: res.recoveryToken });
      onClose();
      go(`/class/${res.classId}`);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Unbekannter Fehler');
      setBusy(false);
    }
  }

  return (
    <Sheet
      title="Neue Klasse"
      onClose={onClose}
      action={<button className="btn btn-plain strong" type="button" disabled={!name.trim() || busy} onClick={create}>Erstellen</button>}
    >
      <div className="field">
        <label htmlFor="f-name">Klassenname</label>
        <input className="input" id="f-name" value={name} maxLength={60} autoFocus placeholder="z. B. 7b · Informatik" onChange={(e) => setName(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor="f-teacher">Dein Name (optional)</label>
        <input className="input" id="f-teacher" value={teacher} maxLength={60} placeholder="z. B. Frau Berger" onChange={(e) => setTeacher(e.target.value)} />
        <p className="hint">Wird Schülern angezeigt, wenn du ihren Bildschirm sehen möchtest.</p>
      </div>
      <div className="field">
        <label>Code läuft ab</label>
        <Segmented value={ttl} options={TTL_OPTIONS} onChange={setTtl} label="Code läuft ab" />
        <p className="hint">Bereits beigetretene Schüler bleiben verbunden, auch wenn der Code abläuft.</p>
      </div>
      {error && <p className="error-text">{error}</p>}
    </Sheet>
  );
}

export function Start({ notice }: { notice: string | null }) {
  const local = useLocalState();
  const [infos, setInfos] = useState<Record<string, Info>>({});
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    local.classes.forEach((c) => {
      api.snapshot(c.id, c.teacherToken).then(
        (snap) => {
          store.renameClass(c.id, snap.name);
          setInfos((m) => ({ ...m, [c.id]: { snap } }));
        },
        (e) => {
          const msg = e instanceof ApiError && (e.status === 401 || e.status === 403 || e.status === 404)
            ? 'Kein Zugriff mehr auf diese Klasse' : e instanceof ApiError ? e.message : 'Fehler';
          setInfos((m) => ({ ...m, [c.id]: { error: msg } }));
        },
      );
    });
  }, [local.classes.length]);

  return (
    <div className="wrap">
      <div className="navbar">
        <div className="grow"><h1 className="large-title">ClassroomView</h1></div>
        <button className="btn btn-primary" type="button" disabled={!local.inviteKey} onClick={() => setCreating(true)}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" aria-hidden="true"><path d="M12 5v14M5 12h14" /></svg>
          Neue Klasse
        </button>
      </div>
      {notice && <div className="banner ok">{notice}</div>}
      {!local.inviteKey && (
        <div className="banner">
          Zum Anlegen von Klassen brauchst du einmal den Einladungslink aus der Host-App. Öffne ihn auf diesem Gerät.
        </div>
      )}
      {local.classes.length > 0 ? (
        <>
          <p className="eyebrow list-label">Klassen auf diesem Gerät</p>
          <div className="class-list">
            {[...local.classes].sort((a, b) => a.name.localeCompare(b.name, 'de')).map((c) => (
              <ClassCard key={c.id} c={c} info={infos[c.id]} />
            ))}
          </div>
        </>
      ) : (
        <div className="empty card">
          <h2>Noch keine Klasse</h2>
          <p>Erstelle eine Klasse. Du bekommst einen Code, mit dem Schüler in der ClassroomView-App beitreten.</p>
        </div>
      )}
      <p className="empty-hint">
        Klassen werden nur auf diesem Gerät gespeichert. Für ein anderes Gerät gibt es in den Klasseneinstellungen
        einen Wiederherstellungslink.
      </p>
      {creating && local.inviteKey && <CreateSheet inviteKey={local.inviteKey} onClose={() => setCreating(false)} />}
    </div>
  );
}

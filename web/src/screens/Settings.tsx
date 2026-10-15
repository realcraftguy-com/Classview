import { useEffect, useState } from 'react';
import { ApiError, api } from '../api/client';
import type { CodeTtl, TeacherDto } from '../api/types';
import { BackButton, Segmented, TTL_OPTIONS, Toast, Toggle, copyText, formatWhen, useFlash } from '../components/ui';
import { go } from '../router';
import { store, type SavedClass } from '../store';
import { NoAccess, useClass } from './ClassView';

export function Settings({ saved }: { saved: SavedClass }) {
  const { snap, setSnap, error } = useClass(saved);
  const [teachers, setTeachers] = useState<TeacherDto[]>([]);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [confirmRecovery, setConfirmRecovery] = useState(false);
  const [flash, setFlash] = useFlash();

  useEffect(() => {
    api.teachers(saved.id, saved.teacherToken).then(setTeachers, () => {});
  }, [saved.id, saved.teacherToken]);

  if (error && !snap) return <NoAccess saved={saved} error={error} />;
  if (!snap) return null;

  const fail = (e: unknown) => setFlash(e instanceof ApiError ? e.message : 'Fehler');
  const recoveryLink = saved.recoveryToken ? `${location.origin}/#/recover/${saved.recoveryToken}` : null;

  return (
    <div className="wrap">
      <div className="navbar"><BackButton label={snap.name} onClick={() => go(`/class/${saved.id}`)} /></div>
      <div className="settings">
        <h1 className="large-title">Einstellungen</h1>

        <section>
          <p className="eyebrow group-label">Beitreten</p>
          <div className="card group">
            <div className="row">
              <div className="grow">Beitritt pausieren<small>Der Code funktioniert vorübergehend nicht.</small></div>
              <Toggle label="Beitritt pausieren" checked={snap.joinPaused}
                onChange={(v) => api.update(saved.id, saved.teacherToken, { joinPaused: v }).then(setSnap, fail)} />
            </div>
            <div className="row column">
              <div className="grow">Code läuft ab<small>{snap.codeExpiresAt ? formatWhen(snap.codeExpiresAt) : 'nie'} · Änderung gilt ab jetzt</small></div>
              <Segmented<CodeTtl> label="Code läuft ab" value={snap.codeTtl} options={TTL_OPTIONS}
                onChange={(v) => api.update(saved.id, saved.teacherToken, { codeTtl: v }).then(setSnap, fail)} />
            </div>
          </div>
        </section>

        <section>
          <p className="eyebrow group-label">Auf einem anderen Gerät öffnen</p>
          <div className="card group">
            <div className="row">
              <div className="grow">Wiederherstellungslink
                <small className="link-box">{recoveryLink ? recoveryLink.replace(/recover\/.*/, 'recover/••••••••') : 'Auf diesem Gerät nicht gespeichert – erneuere den Link.'}</small>
              </div>
              {recoveryLink && (
                <button className="btn btn-plain" type="button" onClick={async () => setFlash((await copyText(recoveryLink)) ? 'Link kopiert' : 'Kopieren nicht möglich')}>Kopieren</button>
              )}
            </div>
            <div className="row">
              <div className="grow">Link erneuern<small>Der alte Link funktioniert danach nicht mehr.</small></div>
              {confirmRecovery ? (
                <button className="btn btn-danger" type="button" onClick={async () => {
                  try {
                    const r = await api.rotateRecovery(saved.id, saved.teacherToken);
                    store.upsertClass({ ...saved, recoveryToken: r.recoveryToken });
                    setFlash('Neuer Link erstellt');
                  } catch (e) { fail(e); }
                  setConfirmRecovery(false);
                }}>Jetzt erneuern</button>
              ) : (
                <button className="btn btn-plain" type="button" onClick={() => setConfirmRecovery(true)}>Erneuern</button>
              )}
            </div>
          </div>
          <p className="hint group-hint">Wer diesen Link hat, kann die Klasse verwalten. Behandle ihn wie ein Passwort.</p>
        </section>

        <section>
          <p className="eyebrow group-label">Angemeldete Lehrer-Geräte</p>
          <div className="card group">
            {teachers.map((t) => (
              <div className="row" key={t.id}>
                <div className="grow">{t.label}<small>{t.current ? 'dieses Gerät' : t.lastUsedAt ? `zuletzt ${formatWhen(t.lastUsedAt)}` : ''}</small></div>
                {!t.current && (
                  <button className="btn btn-plain danger" type="button" onClick={() =>
                    api.revokeTeacher(saved.id, saved.teacherToken, t.id).then(() => setTeachers(teachers.filter((x) => x.id !== t.id)), fail)}>
                    Abmelden
                  </button>
                )}
              </div>
            ))}
          </div>
        </section>

        {confirmDelete ? (
          <div className="confirm card">
            <p><strong>{snap.name}</strong> mit allen Schülern und Profilbildern endgültig löschen?</p>
            <div className="row-gap">
              <button className="btn btn-tonal" type="button" onClick={() => setConfirmDelete(false)}>Abbrechen</button>
              <button className="btn btn-danger" type="button" onClick={() =>
                api.deleteClass(saved.id, saved.teacherToken).then(() => { store.removeClass(saved.id); go('/'); }, fail)}>
                Endgültig löschen
              </button>
            </div>
          </div>
        ) : (
          <button className="btn btn-danger" type="button" onClick={() => setConfirmDelete(true)}>Klasse löschen</button>
        )}
      </div>
      <Toast message={flash} />
    </div>
  );
}

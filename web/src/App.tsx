import { useEffect, useState } from 'react';
import { ApiError, api } from './api/client';
import { Spinner } from './components/ui';
import { go, replace, useRoute } from './router';
import { ClassView, CodeFullscreen } from './screens/ClassView';
import { Settings } from './screens/Settings';
import { Start } from './screens/Start';
import { store, useLocalState } from './store';

export function App() {
  const route = useRoute();
  const local = useLocalState();
  const [notice, setNotice] = useState<string | null>(null);
  const [recoverError, setRecoverError] = useState<string | null>(null);

  // Einmal-Links einlösen und sofort aus Adresse und Verlauf entfernen.
  useEffect(() => {
    if (route.name === 'invite') {
      store.setInviteKey(route.key);
      setNotice('Einladung gespeichert. Du kannst jetzt Klassen anlegen.');
      replace('/');
    } else if (route.name === 'recover') {
      const token = route.token;
      replace('/');
      api.recover(token).then(
        (r) => {
          store.upsertClass({ id: r.classId, name: r.name, teacherToken: r.teacherToken, recoveryToken: token });
          go(`/class/${r.classId}`);
        },
        (e) => setRecoverError(e instanceof ApiError ? e.message : 'Wiederherstellen fehlgeschlagen'),
      );
    }
  }, [route]);

  if (route.name === 'invite' || route.name === 'recover') {
    return <div className="wrap center-page"><Spinner /></div>;
  }
  if (route.name === 'start') {
    return <Start notice={recoverError ?? notice} />;
  }
  const saved = local.classes.find((c) => c.id === route.id);
  if (!saved) {
    return (
      <div className="wrap">
        <div className="empty card">
          <h2>Klasse nicht auf diesem Gerät</h2>
          <p>Öffne den Wiederherstellungslink der Klasse, um sie hier zu verwalten.</p>
          <button className="btn btn-primary" type="button" onClick={() => go('/')}>Zur Übersicht</button>
        </div>
      </div>
    );
  }
  if (route.name === 'code') return <CodeFullscreen saved={saved} />;
  if (route.name === 'settings') return <Settings saved={saved} />;
  return <ClassView saved={saved} />;
}

import { useSyncExternalStore } from 'react';

/**
 * Hash-Routen: #/  ·  #/class/<id>  ·  #/class/<id>/code  ·  #/class/<id>/settings
 * Einmal-Links: #/invite/<schlüssel>  ·  #/recover/<token>  (werden nach dem Einlösen aus der Adresse entfernt)
 */
export type Route =
  | { name: 'start' }
  | { name: 'class'; id: string }
  | { name: 'code'; id: string }
  | { name: 'settings'; id: string }
  | { name: 'invite'; key: string }
  | { name: 'recover'; token: string };

export function parse(hash: string): Route {
  const parts = hash.replace(/^#\/?/, '').split('/').filter(Boolean).map(decodeURIComponent);
  if (parts[0] === 'class' && parts[1]) {
    if (parts[2] === 'code') return { name: 'code', id: parts[1] };
    if (parts[2] === 'settings') return { name: 'settings', id: parts[1] };
    return { name: 'class', id: parts[1] };
  }
  if (parts[0] === 'invite' && parts[1]) return { name: 'invite', key: parts[1] };
  if (parts[0] === 'recover' && parts[1]) return { name: 'recover', token: parts[1] };
  return { name: 'start' };
}

export function useRoute(): Route {
  const hash = useSyncExternalStore(
    (l) => {
      window.addEventListener('hashchange', l);
      return () => window.removeEventListener('hashchange', l);
    },
    () => location.hash,
  );
  return parse(hash);
}

export function go(path: string) {
  location.hash = path;
}

/** Ersetzt die Adresse ohne Verlaufseintrag – für Einmal-Links, damit Geheimnisse nicht im Verlauf bleiben. */
export function replace(path: string) {
  history.replaceState(null, '', path.startsWith('#') ? path : '#' + path);
  window.dispatchEvent(new HashChangeEvent('hashchange'));
}

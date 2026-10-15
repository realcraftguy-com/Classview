import { useSyncExternalStore } from 'react';

/**
 * Was die Web-App auf diesem Gerät speichert (localStorage). Achtung: Home-Bildschirm-App und Safari-Tab
 * haben getrennten Speicher; Safari kann ihn nach 7 Tagen ohne Nutzung löschen (siehe docs/security.md).
 */
export interface SavedClass {
  id: string;
  name: string;
  teacherToken: string;
  recoveryToken?: string;
}

export interface LocalState {
  inviteKey?: string;
  classes: SavedClass[];
}

const KEY = 'classview.v1';
const listeners = new Set<() => void>();

function read(): LocalState {
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) return JSON.parse(raw) as LocalState;
  } catch {
    /* privat/gesperrt */
  }
  return { classes: [] };
}

let state: LocalState = read();

function write(next: LocalState) {
  state = next;
  try {
    localStorage.setItem(KEY, JSON.stringify(next));
  } catch {
    /* Speicher nicht verfügbar – bleibt im Speicher dieser Sitzung */
  }
  listeners.forEach((l) => l());
}

export const store = {
  get: () => state,
  setInviteKey(inviteKey: string) {
    write({ ...state, inviteKey });
  },
  upsertClass(c: SavedClass) {
    const others = state.classes.filter((x) => x.id !== c.id);
    const existing = state.classes.find((x) => x.id === c.id);
    write({ ...state, classes: [...others, { ...existing, ...c }] });
  },
  renameClass(id: string, name: string) {
    const c = state.classes.find((x) => x.id === id);
    if (c && c.name !== name) store.upsertClass({ ...c, name });
  },
  removeClass(id: string) {
    write({ ...state, classes: state.classes.filter((x) => x.id !== id) });
  },
  findClass: (id: string) => state.classes.find((x) => x.id === id),
};

export function useLocalState(): LocalState {
  return useSyncExternalStore(
    (l) => {
      listeners.add(l);
      return () => listeners.delete(l);
    },
    () => state,
  );
}

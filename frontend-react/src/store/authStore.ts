import { create } from 'zustand';

const TOKEN_KEY = 'hedwig_token';
const USERNAME_KEY = 'hedwig_username';

interface AuthStore {
  token: string | null;
  username: string | null;
  isAuthenticated: () => boolean;
  login: (token: string, username: string) => void;
  logout: () => void;
  getToken: () => string | null;
}

export const useAuthStore = create<AuthStore>((set, get) => ({
  token: localStorage.getItem(TOKEN_KEY),
  username: localStorage.getItem(USERNAME_KEY),
  isAuthenticated: () => get().token !== null,
  login: (token, username) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USERNAME_KEY, username);
    set({ token, username });
  },
  logout: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    set({ token: null, username: null });
  },
  getToken: () => get().token,
}));

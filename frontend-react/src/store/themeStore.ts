import { create } from 'zustand';

const THEME_KEY = 'hedwig_theme';

interface ThemeStore {
  isDark: boolean;
  toggle: () => void;
}

export const useThemeStore = create<ThemeStore>((set, get) => ({
  isDark: localStorage.getItem(THEME_KEY) !== 'light',
  toggle: () => {
    const next = !get().isDark;
    localStorage.setItem(THEME_KEY, next ? 'dark' : 'light');
    set({ isDark: next });
  },
}));

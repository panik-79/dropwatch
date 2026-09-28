import { Injectable, signal, effect } from '@angular/core';
import { THEME_STORAGE_KEY, type Theme } from '../constants';

/**
 * ThemeService — manages dark/light mode as a global signal.
 * Persists preference to localStorage and applies it to the <html> element
 * via a `data-theme` attribute, which CSS variables in styles.css respond to.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  /** Current theme. Default to saved preference, or 'dark'. */
  readonly theme = signal<Theme>(this.loadSavedTheme());

  readonly isDark = signal<boolean>(this.theme() === 'dark');

  constructor() {
    // Apply theme immediately on construction (before first paint)
    this.applyTheme(this.theme());

    // Reactively apply whenever theme changes
    effect(() => {
      const t = this.theme();
      this.isDark.set(t === 'dark');
      this.applyTheme(t);
      this.saveTheme(t);
    });
  }

  toggle(): void {
    this.theme.update(current => (current === 'dark' ? 'light' : 'dark'));
  }

  setTheme(theme: Theme): void {
    this.theme.set(theme);
  }

  private applyTheme(theme: Theme): void {
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-theme', theme);
    }
  }

  private saveTheme(theme: Theme): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(THEME_STORAGE_KEY, theme);
    }
  }

  private loadSavedTheme(): Theme {
    if (typeof localStorage === 'undefined') return 'dark';
    const saved = localStorage.getItem(THEME_STORAGE_KEY);
    return (saved === 'light' || saved === 'dark') ? saved : 'dark';
  }
}

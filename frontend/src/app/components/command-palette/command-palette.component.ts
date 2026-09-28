import { Component, EventEmitter, Output, HostListener, inject, computed, signal, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TrackerStore } from '../../services/tracker.store';
import { TrackerSelectionService } from '../../services/tracker-selection.service';
import { type TrackerItem } from '../../models/tracker.models';

interface CommandItem {
  type: 'nav' | 'tracker';
  label: string;
  sub?: string;
  icon?: string;
  action: () => void;
}

@Component({
  selector: 'app-command-palette',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="palette-overlay" (click)="close.emit()">
      <div class="glass-card palette-content" (click)="$event.stopPropagation()">

        <!-- Search Input -->
        <div style="display: flex; align-items: center; gap: 12px; padding: 16px 20px; border-bottom: 1px solid var(--card-border);">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--accent-primary)" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
          <input
            #searchInput
            type="text"
            [(ngModel)]="query"
            (ngModelChange)="onQueryChange()"
            placeholder="Type a command or search trackers..."
            class="palette-input"
            autocomplete="off"
          />
          @if (query) {
            <button (click)="query = ''; onQueryChange()" class="btn-ghost" style="padding: 4px 6px; font-size: 1rem; color: var(--text-dim);">&times;</button>
          }
          <span style="font-size: 0.72rem; background: var(--btn-secondary-bg); color: var(--text-muted); padding: 3px 8px; border-radius: 6px; font-family: var(--font-mono); white-space: nowrap; border: 1px solid var(--card-border);">ESC</span>
        </div>

        <!-- Results List -->
        <div #resultsList style="max-height: 380px; overflow-y: auto; padding: 8px;">

          @if (filteredCommands().length === 0) {
            <div style="text-align: center; padding: 32px 16px; color: var(--text-dim); font-size: 0.88rem;">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="margin-bottom: 8px; opacity: 0.5;"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
              <div>No results for "{{ query }}"</div>
            </div>
          } @else {
            @let navItems = filteredCommands().filter(c => c.type === 'nav');
            @let trackerItems = filteredCommands().filter(c => c.type === 'tracker');

            @if (navItems.length > 0) {
              <div class="section-header">Navigation</div>
              @for (item of navItems; track item.label; let i = $index) {
                <div
                  [class]="'palette-item' + (focusedIndex === getNavIndex(i) ? ' palette-item--focused' : '')"
                  (click)="executeItem(item)"
                  (mouseenter)="focusedIndex = getNavIndex(i)"
                >
                  <span style="display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0;">
                    <span style="color: var(--accent-primary); flex-shrink: 0;" [innerHTML]="item.icon || ''"></span>
                    <span>{{ item.label }}</span>
                  </span>
                  @if (focusedIndex === getNavIndex(i)) {
                    <span style="font-size: 0.68rem; background: var(--btn-secondary-bg); border: 1px solid var(--card-border); padding: 2px 7px; border-radius: 4px; font-family: var(--font-mono); color: var(--text-dim);">Enter ↵</span>
                  }
                </div>
              }
            }

            @if (trackerItems.length > 0) {
              <div class="section-header" style="margin-top: 6px;">Tracked Products</div>
              @for (item of trackerItems; track item.label; let i = $index) {
                <div
                  [class]="'palette-item' + (focusedIndex === getTrackerIndex(i, navItems.length) ? ' palette-item--focused' : '')"
                  (click)="executeItem(item)"
                  (mouseenter)="focusedIndex = getTrackerIndex(i, navItems.length)"
                >
                  <span style="display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0;">
                    <span class="badge" [class]="item.sub === 'MYNTRA' ? 'badge-myntra' : 'badge-flipkart'" style="flex-shrink: 0;">{{ item.sub }}</span>
                    <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">{{ item.label }}</span>
                  </span>
                  @if (focusedIndex === getTrackerIndex(i, navItems.length)) {
                    <span style="font-size: 0.68rem; background: var(--btn-secondary-bg); border: 1px solid var(--card-border); padding: 2px 7px; border-radius: 4px; font-family: var(--font-mono); color: var(--text-dim);">Enter ↵</span>
                  }
                </div>
              }
            }
          }
        </div>

        <!-- Footer Hint -->
        <div style="padding: 10px 16px; border-top: 1px solid var(--card-border); display: flex; gap: 16px; font-size: 0.7rem; color: var(--text-dim);">
          <span><kbd style="background: var(--btn-secondary-bg); border: 1px solid var(--card-border); border-radius: 4px; padding: 1px 5px; font-family: var(--font-mono);">↑↓</kbd> Navigate</span>
          <span><kbd style="background: var(--btn-secondary-bg); border: 1px solid var(--card-border); border-radius: 4px; padding: 1px 5px; font-family: var(--font-mono);">↵</kbd> Select</span>
          <span><kbd style="background: var(--btn-secondary-bg); border: 1px solid var(--card-border); border-radius: 4px; padding: 1px 5px; font-family: var(--font-mono);">ESC</kbd> Close</span>
        </div>

      </div>
    </div>
  `,
  styles: [`
    .palette-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: var(--palette-overlay-bg);
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
      z-index: 2000;
      display: flex;
      justify-content: center;
      padding-top: 100px;
      animation: fadeIn 0.15s ease-out;
    }
    .palette-content {
      width: 100%;
      max-width: 640px;
      height: fit-content;
      padding: 0;
      overflow: hidden;
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5), 0 0 24px var(--accent-glow);
      animation: scaleUp 0.18s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .palette-input {
      background: transparent;
      border: none;
      outline: none;
      color: var(--text-main);
      font-size: 1.05rem;
      font-family: var(--font-body);
      width: 100%;
      caret-color: var(--accent-primary);
    }
    .palette-input::placeholder { color: var(--text-dim); }
    .section-header {
      padding: 6px 12px;
      font-size: 0.7rem;
      font-weight: 700;
      color: var(--text-dim);
      text-transform: uppercase;
      letter-spacing: 0.6px;
      margin-bottom: 2px;
    }
    .palette-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      padding: 10px 14px;
      border-radius: 8px;
      cursor: pointer;
      color: var(--text-muted);
      font-size: 0.92rem;
      transition: background-color 0.1s ease, color 0.1s ease;
    }
    .palette-item:hover,
    .palette-item--focused {
      background: var(--palette-item-hover);
      color: var(--text-main);
    }
  `]
})
export class CommandPaletteComponent implements AfterViewInit {
  @Output() close = new EventEmitter<void>();
  @ViewChild('searchInput') searchInputRef!: ElementRef<HTMLInputElement>;

  private readonly router = inject(Router);
  readonly trackerStore = inject(TrackerStore);
  private readonly trackerSelectionService = inject(TrackerSelectionService);

  query = '';
  focusedIndex = 0;

  private readonly navCommands: CommandItem[] = [
    {
      type: 'nav',
      label: 'Go to Dashboard',
      icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>',
      action: () => this.navigate('/'),
    },
    {
      type: 'nav',
      label: 'Manage Feature Flags',
      icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>',
      action: () => this.navigate('/flags'),
    },
    {
      type: 'nav',
      label: 'Notification & Telegram Settings',
      icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>',
      action: () => this.navigate('/settings'),
    },
  ];

  readonly filteredCommands = computed(() => {
    const q = this.query.toLowerCase().trim();

    const navItems: CommandItem[] = !q
      ? this.navCommands
      : this.navCommands.filter(c => c.label.toLowerCase().includes(q));

    const trackerItems: CommandItem[] = this.trackerStore.trackers()
      .filter(t => {
        if (!q) return true;
        const searchStr = [t.product?.title, t.product?.brand, t.product?.site].join(' ').toLowerCase();
        return searchStr.includes(q);
      })
      .slice(0, 8) // cap at 8 results
      .map(t => ({
        type: 'tracker' as const,
        label: t.product?.title || 'Tracked Product',
        sub: t.product?.site || 'MERCHANT',
        action: () => this.openTracker(t),
      }));

    return [...navItems, ...trackerItems];
  });

  ngAfterViewInit(): void {
    setTimeout(() => this.searchInputRef?.nativeElement?.focus(), 50);
  }

  onQueryChange(): void {
    this.focusedIndex = 0;
  }

  getNavIndex(i: number): number { return i; }
  getTrackerIndex(i: number, navCount: number): number { return navCount + i; }

  executeItem(item: CommandItem): void {
    item.action();
  }

  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    const total = this.filteredCommands().length;
    if (total === 0) return;

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.focusedIndex = (this.focusedIndex + 1) % total;
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.focusedIndex = (this.focusedIndex - 1 + total) % total;
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const item = this.filteredCommands()[this.focusedIndex];
      if (item) item.action();
    } else if (event.key === 'Escape') {
      this.close.emit();
    }
  }

  private navigate(path: string): void {
    this.router.navigate([path]);
    this.close.emit();
  }

  private openTracker(t: TrackerItem): void {
    this.router.navigate(['/']);
    this.trackerSelectionService.select(t);
    this.close.emit();
  }
}

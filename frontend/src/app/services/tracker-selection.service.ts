import { Injectable, signal } from '@angular/core';
import type { TrackerItem } from '../models/tracker.models';

/**
 * TrackerSelectionService — lightweight signal bus for cross-component
 * tracker selection. Allows the CommandPalette to open the detail modal
 * on the Dashboard without tight coupling.
 */
@Injectable({ providedIn: 'root' })
export class TrackerSelectionService {
  /** The tracker to open in the detail modal. Null when nothing selected. */
  readonly selectedTracker = signal<TrackerItem | null>(null);

  select(tracker: TrackerItem): void {
    this.selectedTracker.set(tracker);
  }

  clear(): void {
    this.selectedTracker.set(null);
  }
}

import { Injectable } from '@angular/core';

/**
 * SoundService — plays UI feedback sounds using the Web Audio API.
 * Zero external dependencies. No audio files needed.
 * All methods are no-ops in environments without AudioContext.
 */
@Injectable({ providedIn: 'root' })
export class SoundService {
  private ctx: AudioContext | null = null;
  private enabled = true;

  setEnabled(enabled: boolean): void {
    this.enabled = enabled;
  }

  /** Pleasant upward arpeggio — used for price drop alerts */
  playPriceDropAlert(): void {
    this.playNotes([523.25, 659.25, 783.99], 0.12, 0.08, 'sine');
  }

  /** Soft confirmation ding — used for tracker created, settings saved */
  playSuccess(): void {
    this.playNotes([659.25, 880], 0.08, 0.06, 'sine');
  }

  /** Subtle warning tone */
  playWarning(): void {
    this.playNotes([392, 349.23], 0.08, 0.08, 'triangle');
  }

  private getContext(): AudioContext | null {
    if (!this.enabled) return null;
    if (typeof window === 'undefined' || !('AudioContext' in window || 'webkitAudioContext' in window)) {
      return null;
    }
    if (!this.ctx || this.ctx.state === 'closed') {
      this.ctx = new AudioContext();
    }
    return this.ctx;
  }

  private playNotes(
    frequencies: number[],
    gainValue: number,
    noteGap: number,
    type: OscillatorType
  ): void {
    const ctx = this.getContext();
    if (!ctx) return;

    const now = ctx.currentTime;
    frequencies.forEach((freq, i) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.type = type;
      osc.frequency.setValueAtTime(freq, now + i * noteGap);

      gain.gain.setValueAtTime(gainValue, now + i * noteGap);
      gain.gain.exponentialRampToValueAtTime(0.001, now + i * noteGap + 0.35);

      osc.start(now + i * noteGap);
      osc.stop(now + i * noteGap + 0.35);
    });
  }
}

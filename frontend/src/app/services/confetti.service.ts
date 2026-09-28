import { Injectable } from '@angular/core';

/**
 * ConfettiService — launches a canvas-based confetti burst.
 * Uses requestAnimationFrame for smooth 60fps rendering.
 * Pure implementation, no external libraries.
 */
@Injectable({ providedIn: 'root' })
export class ConfettiService {
  private canvas: HTMLCanvasElement | null = null;
  private ctx2d: CanvasRenderingContext2D | null = null;
  private particles: Particle[] = [];
  private rafId: number | null = null;

  private readonly COLORS = [
    '#6366F1', '#818CF8', '#34D399', '#FB7185', '#FBBF24',
    '#38BDF8', '#A78BFA', '#F472B6', '#4ADE80',
  ];

  /** Fire confetti from a given screen point (defaults to top-center). */
  burst(originX?: number, originY?: number): void {
    if (typeof document === 'undefined') return;
    this.ensureCanvas();
    const x = originX ?? (window.innerWidth / 2);
    const y = originY ?? (window.innerHeight * 0.25);

    for (let i = 0; i < 80; i++) {
      this.particles.push(this.createParticle(x, y));
    }

    if (!this.rafId) {
      this.animate();
    }
  }

  private ensureCanvas(): void {
    if (this.canvas) return;

    this.canvas = document.createElement('canvas');
    this.canvas.style.cssText = `
      position: fixed; top: 0; left: 0; width: 100%; height: 100%;
      pointer-events: none; z-index: 99999;
    `;
    this.canvas.width = window.innerWidth;
    this.canvas.height = window.innerHeight;
    document.body.appendChild(this.canvas);
    this.ctx2d = this.canvas.getContext('2d');

    window.addEventListener('resize', () => {
      if (this.canvas) {
        this.canvas.width = window.innerWidth;
        this.canvas.height = window.innerHeight;
      }
    }, { passive: true });
  }

  private createParticle(x: number, y: number): Particle {
    const angle = Math.random() * Math.PI * 2;
    const speed = Math.random() * 8 + 3;
    return {
      x, y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed - (Math.random() * 5 + 3),
      color: this.COLORS[Math.floor(Math.random() * this.COLORS.length)],
      size: Math.random() * 8 + 4,
      rotation: Math.random() * 360,
      rotationSpeed: (Math.random() - 0.5) * 8,
      gravity: 0.25,
      alpha: 1,
      shape: Math.random() > 0.5 ? 'rect' : 'circle',
    };
  }

  private animate(): void {
    const ctx = this.ctx2d;
    if (!ctx || !this.canvas) return;

    ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

    this.particles = this.particles.filter(p => p.alpha > 0.01);

    for (const p of this.particles) {
      p.x += p.vx;
      p.y += p.vy;
      p.vy += p.gravity;
      p.vx *= 0.99;
      p.rotation += p.rotationSpeed;
      p.alpha -= 0.013;

      ctx.save();
      ctx.translate(p.x, p.y);
      ctx.rotate((p.rotation * Math.PI) / 180);
      ctx.globalAlpha = Math.max(0, p.alpha);
      ctx.fillStyle = p.color;

      if (p.shape === 'circle') {
        ctx.beginPath();
        ctx.arc(0, 0, p.size / 2, 0, Math.PI * 2);
        ctx.fill();
      } else {
        ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2);
      }
      ctx.restore();
    }

    if (this.particles.length > 0) {
      this.rafId = requestAnimationFrame(() => this.animate());
    } else {
      this.rafId = null;
    }
  }
}

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  color: string;
  size: number;
  rotation: number;
  rotationSpeed: number;
  gravity: number;
  alpha: number;
  shape: 'rect' | 'circle';
}

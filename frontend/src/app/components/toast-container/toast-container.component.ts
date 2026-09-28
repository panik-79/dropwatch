import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-wrapper">
      @for (t of toastService.toasts(); track t.id) {
        <div [class]="'toast-card toast-' + t.type">
          <div class="toast-icon">
            @if (t.type === 'success') {
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#10B981" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            } @else if (t.type === 'error') {
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#F43F5E" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            } @else if (t.type === 'warning') {
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#F59E0B" stroke-width="2.5"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            } @else {
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#00F2FE" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            }
          </div>
          <div class="toast-content">
            <div class="toast-title">{{ t.title }}</div>
            @if (t.message) {
              <div class="toast-msg">{{ t.message }}</div>
            }
          </div>
          <button (click)="toastService.remove(t.id)" class="toast-close">&times;</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-wrapper {
      position: fixed;
      top: 24px;
      right: 24px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-width: 380px;
      width: calc(100vw - 48px);
      pointer-events: none;
    }
    .toast-card {
      pointer-events: auto;
      background: rgba(15, 23, 42, 0.92);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border-radius: 12px;
      padding: 14px 16px;
      display: flex;
      align-items: flex-start;
      gap: 12px;
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.5);
      animation: slideIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
      border: 1px solid rgba(255, 255, 255, 0.1);
    }
    .toast-success { border-left: 4px solid #10B981; }
    .toast-error { border-left: 4px solid #F43F5E; }
    .toast-warning { border-left: 4px solid #F59E0B; }
    .toast-info { border-left: 4px solid #00F2FE; }

    .toast-icon {
      margin-top: 2px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .toast-content {
      flex: 1;
    }
    .toast-title {
      font-family: var(--font-heading);
      font-weight: 700;
      font-size: 0.9rem;
      color: #FFF;
      line-height: 1.2;
    }
    .toast-msg {
      font-size: 0.8rem;
      color: var(--text-muted);
      margin-top: 4px;
      line-height: 1.3;
    }
    .toast-close {
      background: none;
      border: none;
      color: var(--text-dim);
      font-size: 1.3rem;
      cursor: pointer;
      line-height: 1;
      padding: 0 4px;
    }
    .toast-close:hover {
      color: #FFF;
    }

    @keyframes slideIn {
      from { opacity: 0; transform: translateX(30px) scale(0.95); }
      to { opacity: 1; transform: translateX(0) scale(1); }
    }
  `]
})
export class ToastContainerComponent {
  toastService = inject(ToastService);
}

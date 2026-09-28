import { Component, HostListener, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';
import { AddTrackerWizardComponent } from './components/add-tracker-wizard/add-tracker-wizard.component';
import { CommandPaletteComponent } from './components/command-palette/command-palette.component';
import { ToastContainerComponent } from './components/toast-container/toast-container.component';
import { SseService } from './services/sse.service';
import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    NavbarComponent,
    AddTrackerWizardComponent,
    CommandPaletteComponent,
    ToastContainerComponent,
  ],
  template: `
    <div style="min-height: 100vh; display: flex; flex-direction: column;">
      <app-toast-container></app-toast-container>
      <app-navbar (openAddModal)="showAddModal = true" (openPalette)="showPalette = true"></app-navbar>

      <main style="flex: 1;">
        <router-outlet></router-outlet>
      </main>

      @if (showAddModal) {
        <app-add-tracker-wizard (close)="showAddModal = false"></app-add-tracker-wizard>
      }

      @if (showPalette) {
        <app-command-palette (close)="showPalette = false"></app-command-palette>
      }
    </div>
  `
})
export class App implements OnInit, OnDestroy {
  private readonly sseService = inject(SseService);
  // ThemeService is injected here to ensure it initializes (applies theme) at app start
  private readonly themeService = inject(ThemeService);

  showAddModal = false;
  showPalette = false;

  ngOnInit(): void {
    this.sseService.connect();
  }

  ngOnDestroy(): void {
    this.sseService.disconnect();
  }

  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent): void {
    // Ctrl+K or Cmd+K opens command palette
    if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
      event.preventDefault();
      this.showPalette = !this.showPalette;
    }
    // ESC closes modals
    if (event.key === 'Escape') {
      if (this.showAddModal) this.showAddModal = false;
      if (this.showPalette) this.showPalette = false;
    }
  }
}

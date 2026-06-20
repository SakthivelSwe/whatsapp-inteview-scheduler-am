import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { MessageTemplate } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { ConfirmService } from '../../shared/components/confirm.dialog';

@Component({
  selector: 'app-templates',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusChipComponent],
  templateUrl: './templates.component.html',
  styleUrls: ['./templates.component.scss']
})
export class TemplatesComponent implements OnInit {
  private api = inject(ApiService);
  private toast = inject(ToastService);
  private confirmSvc = inject(ConfirmService);

  templates: MessageTemplate[] = [];
  selected: MessageTemplate | null = null;
  editing = false;
  saving = false;
  draft: Partial<MessageTemplate> = { name: '', body: '', active: true };

  ngOnInit() { this.load(); }

  load() {
    this.api.listTemplates().subscribe({
      next: (ts) => {
        this.templates = ts;
        if (!this.selected && ts.length) this.select(ts[0]);
      }
    });
  }

  select(t: MessageTemplate) {
    this.selected = t;
    this.editing = false;
    this.draft = { ...t };
  }

  newTemplate() {
    this.selected = null;
    this.editing = true;
    this.draft = { name: '', body: '', active: true };
  }

  edit() {
    if (!this.selected) return;
    this.editing = true;
    this.draft = { ...this.selected };
  }

  cancel() {
    this.editing = false;
    if (this.selected) this.draft = { ...this.selected };
  }

  save() {
    if (!this.draft.name?.trim() || !this.draft.body?.trim()) {
      this.toast.error('Name and body are required.');
      return;
    }
    this.saving = true;
    const obs = this.selected
      ? this.api.updateTemplate(this.selected.id, this.draft)
      : this.api.createTemplate(this.draft);
    obs.subscribe({
      next: (t) => {
        this.saving = false;
        this.editing = false;
        this.toast.success(this.selected ? 'Template updated' : 'Template created');
        this.api.listTemplates().subscribe((ts) => {
          this.templates = ts;
          this.selected = ts.find((x) => x.id === t.id) ?? t;
          this.draft = { ...this.selected! };
        });
      },
      error: (err) => {
        this.saving = false;
        this.toast.error(err?.error?.message ?? 'Save failed');
      }
    });
  }

  remove(t: MessageTemplate, e: Event) {
    e.stopPropagation();
    this.confirmSvc.ask({
      eyebrow: 'Template',
      title: `Delete "${t.name}"?`,
      message: 'This template will be permanently removed. Existing batches that already referenced it are unaffected.',
      confirmText: 'Delete template',
      variant: 'danger',
      icon: 'delete'
    }).subscribe((ok) => {
      if (!ok) return;
      this.api.deleteTemplate(t.id).subscribe({
        next: () => {
          this.toast.success('Template deleted');
          if (this.selected?.id === t.id) this.selected = null;
          this.load();
        },
        error: () => this.toast.error('Delete failed')
      });
    });
  }

  // Render placeholder substitution with sample values for live preview
  renderedPreview(): string {
    const body = this.draft.body || '';
    const sample: Record<string, string> = {
      column_1: 'Godson Robin Raja S',
      column_2: 'Angular Developer',
      column_3: '09th June 2026',
      column_4: '11:30 AM',
      column_5: 'https://meet.google.com/xwe-ivrc-pet',
      candidateName: 'Godson Robin Raja S',
      jobPosition: 'Angular Developer',
      interviewDate: '09th June 2026',
      interviewTime: '11:30 AM',
      meetingLink: 'https://meet.google.com/xwe-ivrc-pet',
      phoneNumber: '+919342627033'
    };
    return body.replace(/\{\{\s*(\w+)\s*\}\}/g, (_, k) => sample[k] ?? `{{${k}}}`);
  }
}


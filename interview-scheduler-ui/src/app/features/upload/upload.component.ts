import { CommonModule } from '@angular/common';
import { Component, ElementRef, ViewChild, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { ToastService } from '../../core/toast.service';
import { UploadResponse } from '../../core/models';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, StatusChipComponent],
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.scss']
})
export class UploadComponent {
  private api = inject(ApiService);
  private toast = inject(ToastService);
  private router = inject(Router);

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  dragOver = false;
  uploading = false;
  selectedFile: File | null = null;
  result: UploadResponse | null = null;

  // Reference data shown to user
  readonly expectedColumns = [
    { header: 'Phone Number',   placeholder: '(system)',      example: '+919342627033' },
    { header: 'Candidate Name', placeholder: '{{column_1}}',  example: 'Godson Robin Raja S' },
    { header: 'Job Position',   placeholder: '{{column_2}}',  example: 'Angular Developer' },
    { header: 'Interview Date', placeholder: '{{column_3}}',  example: '09th June 2026' },
    { header: 'Interview Time', placeholder: '{{column_4}}',  example: '11:30 AM' },
    { header: 'Meeting Link',   placeholder: '{{column_5}}',  example: 'https://meet.google.com/xwe-ivrc-pet' }
  ];

  // Literal example shown in tip text (Angular won't interpret it as a binding)
  readonly customColumnExample = '{{panel_name}}';
  readonly placeholderExample = '{{panel_name}}';

  readonly sampleUrl = `${environment.apiBaseUrl}/sample/excel`;

  onDragOver(e: DragEvent) { e.preventDefault(); this.dragOver = true; }
  onDragLeave(e: DragEvent) { e.preventDefault(); this.dragOver = false; }

  onDrop(e: DragEvent) {
    e.preventDefault();
    this.dragOver = false;
    const f = e.dataTransfer?.files?.[0];
    if (f) this.pickFile(f);
  }

  onFileChange(e: Event) {
    const input = e.target as HTMLInputElement;
    const f = input.files?.[0];
    if (f) this.pickFile(f);
  }

  pickFile(f: File) {
    const name = f.name.toLowerCase();
    if (!name.endsWith('.xlsx') && !name.endsWith('.xls')) {
      this.toast.error('Please select an .xlsx or .xls file.');
      return;
    }
    this.selectedFile = f;
    this.result = null;
  }

  clearFile() {
    this.selectedFile = null;
    this.result = null;
    if (this.fileInput) this.fileInput.nativeElement.value = '';
  }

  triggerPick() { this.fileInput.nativeElement.click(); }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
  }

  upload() {
    if (!this.selectedFile) return;
    this.uploading = true;
    this.api.uploadExcel(this.selectedFile).subscribe({
      next: (res) => {
        this.uploading = false;
        this.result = res;
        this.api.rememberBatch({
          id: res.batchId,
          fileName: res.fileName,
          totalCandidates: res.totalRows,
          status: 'UPLOADED',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        });
        this.toast.success(`Uploaded — ${res.validCandidates} valid, ${res.invalidCandidates} invalid`);
      },
      error: (err) => {
        this.uploading = false;
        this.toast.error(err?.error?.message ?? 'Upload failed. Please verify the file format.');
      }
    });
  }

  goToBatch() {
    if (this.result) this.router.navigate(['/batches', this.result.batchId]);
  }

  copyPlaceholder(p: string) {
    navigator.clipboard?.writeText(p).then(
      () => this.toast.info(`Copied ${p}`),
      () => {}
    );
  }
}


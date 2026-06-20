import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  Batch,
  BatchStatusResponse,
  Candidate,
  ColumnSchema,
  MessageLog,
  MessageTemplate,
  UploadResponse
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  /* ------------------ Candidates / Upload ------------------ */
  uploadExcel(file: File, columnMapping?: Record<string, string>): Observable<UploadResponse> {
    const form = new FormData();
    form.append('file', file);
    if (columnMapping && Object.keys(columnMapping).length > 0) {
      form.append('columnMapping', JSON.stringify(columnMapping));
    }
    return this.http.post<UploadResponse>(`${this.base}/candidates/upload`, form);
  }

  listCandidates(batchId: string): Observable<Candidate[]> {
    return this.http.get<Candidate[]>(`${this.base}/candidates`, { params: { batchId } });
  }

  getBatchSchema(batchId: string): Observable<ColumnSchema[]> {
    return this.http.get<ColumnSchema[]>(`${this.base}/candidates/schema/${batchId}`);
  }

  /* ------------------ Messages ------------------ */
  sendAll(batchId: string, templateName?: string): Observable<BatchStatusResponse> {
    return this.http.post<BatchStatusResponse>(`${this.base}/messages/send-all`, {
      batchId,
      templateName
    });
  }

  retryFailed(batchId: string, templateName?: string): Observable<BatchStatusResponse> {
    return this.http.post<BatchStatusResponse>(`${this.base}/messages/retry-failed`, {
      batchId,
      templateName
    });
  }

  pauseBatch(batchId: string): Observable<BatchStatusResponse> {
    return this.http.post<BatchStatusResponse>(`${this.base}/messages/pause/${batchId}`, {});
  }

  resumeBatch(batchId: string, templateName?: string): Observable<BatchStatusResponse> {
    let url = `${this.base}/messages/resume/${batchId}`;
    if (templateName) {
      url += `?templateName=${encodeURIComponent(templateName)}`;
    }
    return this.http.post<BatchStatusResponse>(url, {});
  }

  getStatus(batchId: string): Observable<BatchStatusResponse> {
    return this.http.get<BatchStatusResponse>(`${this.base}/messages/status/${batchId}`);
  }

  getLogs(batchId: string): Observable<MessageLog[]> {
    return this.http.get<MessageLog[]>(`${this.base}/messages/logs/${batchId}`);
  }

  /* ------------------ Templates ------------------ */
  listTemplates(): Observable<MessageTemplate[]> {
    return this.http.get<MessageTemplate[]>(`${this.base}/templates`);
  }

  createTemplate(t: Partial<MessageTemplate>): Observable<MessageTemplate> {
    return this.http.post<MessageTemplate>(`${this.base}/templates`, t);
  }

  updateTemplate(id: string, t: Partial<MessageTemplate>): Observable<MessageTemplate> {
    return this.http.put<MessageTemplate>(`${this.base}/templates/${id}`, t);
  }

  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/templates/${id}`);
  }

  /* ------------------ Batches ------------------ */
  /** Live list of batches from the backend (DB-backed, source of truth). */
  listBatches(): Observable<Batch[]> {
    return this.http.get<Batch[]>(`${this.base}/batches`);
  }

  deleteBatch(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/batches/${id}`);
  }

  /** Backwards-compatible local cache, used as a fallback if the API is unreachable. */
  listKnownBatches(): Batch[] {
    try {
      return JSON.parse(localStorage.getItem('known_batches') ?? '[]') as Batch[];
    } catch {
      return [];
    }
  }

  rememberBatch(b: Batch): void {
    const all = this.listKnownBatches().filter((x) => x.id !== b.id);
    all.unshift(b);
    localStorage.setItem('known_batches', JSON.stringify(all.slice(0, 50)));
  }

  forgetBatch(id: string): void {
    const all = this.listKnownBatches().filter((x) => x.id !== id);
    localStorage.setItem('known_batches', JSON.stringify(all));
  }

  /* ------------------ WhatsApp bridge status ------------------ */
  getWhatsAppStatus(): Observable<WhatsAppStatus> {
    return this.http.get<WhatsAppStatus>(`${this.base}/whatsapp/status`);
  }

  disconnectWhatsApp(): Observable<{ success: boolean; error?: string }> {
    return this.http.post<{ success: boolean; error?: string }>(`${this.base}/whatsapp/logout`, {});
  }

  resetWhatsApp(): Observable<{ success: boolean; message?: string; error?: string }> {
    return this.http.post<{ success: boolean; message?: string; error?: string }>(`${this.base}/whatsapp/reset`, {});
  }
}

export interface WhatsAppStatus {
  provider: string;
  ready: boolean;
  hasQr: boolean;
  state?: string;
  bridgeUrl?: string;
  qrUrl?: string;
  qrImageUrl?: string;
  info?: { wid?: string; pushname?: string; platform?: string };
  message?: string;
  error?: string;
}

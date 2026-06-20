export type SendStatus = 'PENDING' | 'INVALID' | 'SENT' | 'FAILED' | 'DELIVERED';
export type BatchStatus = 'UPLOADED' | 'SENDING' | 'PAUSED' | 'COMPLETED' | 'FAILED';

export interface Candidate {
  id: string;
  batchId: string;
  rowNumber: number;
  phoneNumber: string;
  candidateName: string;
  jobPosition?: string;
  interviewDate?: string;
  interviewTime?: string;
  meetingLink?: string;
  status: SendStatus;
  validationError?: string;
  lastError?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RowError {
  rowNumber: number;
  candidateName?: string;
  phoneNumber?: string;
  error: string;
}

export interface UploadResponse {
  batchId: string;
  fileName: string;
  totalRows: number;
  validCandidates: number;
  invalidCandidates: number;
  errors: RowError[];
  schema: ColumnSchema[];
}

export interface ColumnSchema {
  header: string;
  slug: string;
  namedPlaceholder: string;
  positionalPlaceholder?: string;
  sampleValue?: string;
  phoneColumn: boolean;
  known: boolean;
}

export interface FailedRecord {
  candidateId: string;
  rowNumber: number;
  candidateName: string;
  phoneNumber: string;
  error: string;
}

export interface BatchStatusResponse {
  batchId: string;
  status: BatchStatus;
  totalRecords: number;
  successfullySent: number;
  failed: number;
  invalid: number;
  pending: number;
  failedRecords: FailedRecord[];
}

export interface Batch {
  id: string;
  fileName: string;
  totalCandidates: number;
  status: BatchStatus;
  createdAt: string;
  updatedAt: string;
}

export interface MessageTemplate {
  id: string;
  name: string;
  body: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface MessageLog {
  id: string;
  batchId: string;
  candidateId: string;
  whatsappNumber: string;
  renderedMessage: string;
  status: 'SENT' | 'FAILED' | 'DELIVERED';
  providerMessageId?: string;
  errorMessage?: string;
  createdAt: string;
}


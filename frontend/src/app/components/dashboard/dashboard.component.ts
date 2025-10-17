import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import {
  CreateDocumentRequest,
  DocumentDto,
  DocumentService,
  UpdateDocumentRequest
} from '../../services/document.service';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  styleUrls: ['./dashboard.component.css'],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  documents: (DocumentDto & { editing?: boolean })[] = [];
  newDocument: CreateDocumentRequest = { fileName: '', summary: '' };
  selectedFile: File | null = null;
  loading = false;
  errorMsg = '';

  constructor(private docService: DocumentService) {}

  ngOnInit(): void {
    this.loadDocuments();
  }

  // Load all documents
  loadDocuments(): void {
    this.loading = true;
    this.errorMsg = '';

    this.docService.getDocuments()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (data) => {
          console.log('[GET /documents] response:', data);
          this.documents = (data ?? []).map((d) => ({ ...d, editing: false }));
        },
        error: (err) => {
          console.error('[GET /documents] error:', err);
          this.errorMsg = 'Failed to load documents.';
        }
      });
  }

  // Handle file selection
  onFileSelected(event: any): void {
    this.selectedFile = event.target.files[0];
  }

  // Upload new document (file + summary)
  uploadDocument(form?: NgForm): void {
    if (!this.selectedFile || !this.newDocument.summary.trim()) {
      alert('Please select a file and enter a summary.');
      return;
    }

    const formData = new FormData();
    formData.append('file', this.selectedFile);
    formData.append('summary', this.newDocument.summary);

    this.loading = true;
    this.docService.uploadDocument(formData)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (_) => {
          this.selectedFile = null;
          this.newDocument.summary = '';
          form?.resetForm();
          this.loadDocuments();
        },
        error: (err) => {
          console.error('[UPLOAD /documents/upload] error:', err);
          this.errorMsg = 'File upload failed.';
        }
      });
  }

  // Edit inline
  editDocument(doc: DocumentDto & { editing?: boolean }): void {
    doc.editing = true;
  }

  // Save inline edit
  saveDocument(doc: DocumentDto & { editing?: boolean }): void {
    if (!doc.id) return;

    const payload: UpdateDocumentRequest = {
      fileName: doc.fileName,
      summary: doc.summary
    };

    this.loading = true;
    this.docService.updateDocument(doc.id, payload)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (_) => this.loadDocuments(),
        error: (err) => console.error('[PUT /documents] error:', err)
      });
  }

  cancelEdit(doc: { editing?: boolean }): void {
    doc.editing = false;
    this.loadDocuments();
  }

  deleteDocument(id?: number): void {
    if (!id) return;

    this.loading = true;
    this.docService.deleteDocument(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (_) => this.loadDocuments(),
        error: (err) => console.error('[DELETE /documents] error:', err)
      });
  }

  trackById = (_: number, item: DocumentDto) => item.id;
}

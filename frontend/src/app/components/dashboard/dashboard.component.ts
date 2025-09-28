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
    loading = false;
    errorMsg = '';

  constructor(private docService: DocumentService) {}

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.loading = true;
    this.errorMsg = '';

    this.docService.getDocuments()
      .pipe(finalize(() => this.loading = false))
      .subscribe({
          next: data => {
            console.log('[GET /documents] response:', data); // 👈 see what you get
            this.documents = (data ?? []).map(d => ({ ...d, editing: false }));
          },
          error: err => {
            console.error('[GET /documents] error:', err);
            this.errorMsg = 'Failed to load documents.';
          }
        });
      }

  addDocument(form?: NgForm): void {
    if (!this.newDocument.fileName?.trim()) return;

    this.loading = true;
    this.docService.addDocument(this.newDocument)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: _ => {
          this.newDocument = { fileName: '', summary: '' };
          form?.resetForm();
          this.loadDocuments(); // always refetch from backend
        },
        error: err => console.error('[POST /documents] error:', err)
      });
  }

  editDocument(doc: DocumentDto & { editing?: boolean }): void {
    doc.editing = true;
  }

  saveDocument(doc: DocumentDto & { editing?: boolean }): void {
    if (!doc.id) return;

    const payload: UpdateDocumentRequest = {
      fileName: doc.fileName,
      summary: doc.summary
    };

    this.loading = true;
    this.docService.updateDocument(doc.id, payload)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: _ => this.loadDocuments(),
        error: err => console.error('[PUT /documents] error:', err)
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
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: _ => this.loadDocuments(),
        error: err => console.error('[DELETE /documents] error:', err)
      });
  }

  trackById = (_: number, item: DocumentDto) => item.id;
}

import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
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
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;
  documents: (DocumentDto & { editing?: boolean })[] = [];
  newDocument: CreateDocumentRequest = { fileName: '', summary: 'PENDING' };
  selectedFile: File | null = null;
  loading = false;
  errorMsg = '';
  searchTerm = '';
  statusFilter: 'ALL' | 'OPEN' | 'IN_REVIEW' | 'REVIEWED' = 'ALL';

  constructor(private docService: DocumentService) {}

  ngOnInit(): void {
    this.loadDocuments();
  }

  // Load all documents
  loadDocuments(): void {
    this.loading = true;
    this.errorMsg = '';

    const statusParam = this.statusFilter === 'ALL' ? undefined : this.statusFilter;

    this.docService.getDocuments(statusParam)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (data) => {
          this.documents = (data ?? []).map((d) => ({
            ...d,
            reviewStatus: d.reviewStatus ?? 'OPEN',
            editing: false
          }));
        },
        error: (err) => {
          console.error('[GET /documents] error:', err);
          this.errorMsg = 'Failed to load documents.';
        }
      });
  }

  // Search a document
  searchDocuments(): void {
    if (!this.searchTerm.trim()) {
      this.loadDocuments();
      return;
    }

    this.loading = true;
    this.docService.searchDocuments(this.searchTerm)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (results) => {
          const mapped = (results ?? []).map((d) => ({
            ...d,
            reviewStatus: d.reviewStatus ?? 'OPEN',
            editing: false
          }));

          if (this.statusFilter === 'ALL') {
            this.documents = mapped;
          } else {
            this.documents = mapped.filter(
              (d) => d.reviewStatus === this.statusFilter
            );
          }
        },
        error: (err) => (this.errorMsg = 'Search failed: ' + err.message)
      });
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.loadDocuments();
  }

  onStatusFilterChange(): void {
    if (this.searchTerm.trim()) {
      this.searchDocuments();
    } else {
      this.loadDocuments();
    }
  }

  // Handle file selection
  onFileSelected(event: any): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files && input.files.length ? input.files[0] : null;
  }

  // Upload new document (file + summary)
  uploadDocument(form?: NgForm): void {
    if (!this.selectedFile) {
      alert('Please select a file.');
      return;
    }

    const formData = new FormData();
    formData.append('file', this.selectedFile);
    formData.append('summary', 'Pending');

    this.loading = true;
    this.docService.uploadDocument(formData)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (_) => {
          this.selectedFile = null;
          this.newDocument.summary = 'Pending';
          if (this.fileInput?.nativeElement) {
            this.fileInput.nativeElement.value = '';
          }
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

  updateReviewStatus(doc: DocumentDto & { editing?: boolean }): void {
    if (!doc.id || !doc.reviewStatus) return;

    this.docService.updateReviewStatus(doc.id, doc.reviewStatus).subscribe({
      next: (_) => {},
      error: (err) => console.error('[PATCH /documents/review-status] error:', err)
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

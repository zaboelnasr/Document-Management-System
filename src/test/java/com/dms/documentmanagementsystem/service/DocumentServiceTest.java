package com.dms.documentmanagementsystem.service;

import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    @Test
    void create_savesDocument() {
        // Arrange (Mock Repo)
        DocumentRepository repo = Mockito.mock(DocumentRepository.class);
        DocumentService service = new DocumentService(repo);

        Document toSave = new Document();
        toSave.setFileName("a.pdf");

        Document saved = new Document();
        saved.setId(1L);
        saved.setFileName("a.pdf");

        when(repo.save(any(Document.class))).thenReturn(saved);

        // Act
        Document result = service.create(toSave);

        // Assert
        assertEquals(1L, result.getId());
        verify(repo).save(any(Document.class));
    }

    @Test
    void getById_notFound_throws() {
        DocumentRepository repo = Mockito.mock(DocumentRepository.class);
        DocumentService service = new DocumentService(repo);

        when(repo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getById(999L));
    }

    @Test
    void update_updatesFields() {
        DocumentRepository repo = Mockito.mock(DocumentRepository.class);
        DocumentService service = new DocumentService(repo);

        Document existing = new Document();
        existing.setId(5L);
        existing.setFileName("old.pdf");
        existing.setSummary("old");

        when(repo.findById(5L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document changes = new Document();
        changes.setFileName("new.pdf");
        changes.setSummary("new");

        Document result = service.update(5L, changes);

        assertEquals("new.pdf", result.getFileName());
        assertEquals("new", result.getSummary());
        verify(repo).save(any(Document.class));
    }

    @Test
    void delete_notFound_throws() {
        DocumentRepository repo = Mockito.mock(DocumentRepository.class);
        DocumentService service = new DocumentService(repo);

        when(repo.existsById(42L)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> service.delete(42L));
        verify(repo, never()).deleteById(anyLong());
    }
}

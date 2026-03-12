package com.smartmail.controller;

import com.smartmail.model.Email;
import com.smartmail.repository.EmailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emails")
public class EmailController {
    
    @Autowired
    private EmailRepository emailRepository;
    
    @GetMapping
    public ResponseEntity<List<Email>> getEmails(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long userId) {
        List<Email> emails;
        if (userId != null) {
            emails = emailRepository.findByUserConfigIdOrderBySentDateDesc(userId);
        } else {
            emails = emailRepository.findAll();
        }
        return ResponseEntity.ok(emails);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Email> getEmail(@PathVariable Long id) {
        return emailRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}/category")
    public ResponseEntity<Email> updateCategory(
            @PathVariable Long id,
            @RequestParam String category) {
        return emailRepository.findById(id)
                .map(email -> {
                    email.setCategory(category);
                    return ResponseEntity.ok(emailRepository.save(email));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmail(@PathVariable Long id) {
        if (emailRepository.existsById(id)) {
            emailRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
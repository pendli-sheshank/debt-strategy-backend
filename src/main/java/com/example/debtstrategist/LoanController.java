package com.example.debtstrategist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/loan") // New base path for the loan analyzer
public class LoanController {

    @Autowired
    private LoanService loanService;

    @PostMapping("/analyze")
    public ResponseEntity<LoanAnalysisResponse> analyzeLoan(@RequestBody LoanAnalysisRequest request) {
        try {
            LoanAnalysisResponse response = loanService.analyzeLoan(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // In LoanController.java, add this new method
    @PostMapping("/analyze-from-prompt")
    public ResponseEntity<LoanAnalysisResponse> analyzeLoanFromPrompt(@RequestBody PromptRequest promptRequest) {
        // This method will call a new service method we are about to create
        try {
            LoanAnalysisResponse response = loanService.analyzeLoanFromPrompt(promptRequest.getPrompt());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            // Consider returning a more specific error response to the frontend
            return ResponseEntity.badRequest().build();
        }
    }
}
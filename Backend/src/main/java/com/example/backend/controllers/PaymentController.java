package com.example.backend.controllers;

import com.example.backend.dtos.Payment.PaymentRequest;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.param.ChargeCreateParams;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController {

    private String currency="USD";

    @PostMapping("/charge")
    public ResponseEntity<String> chargeCard(@RequestBody PaymentRequest paymentRequest) {
        try {
            ChargeCreateParams createParams = new ChargeCreateParams.Builder()
                    .setAmount(paymentRequest.getAmount())
                    .setCurrency(currency)
                    .setSource(paymentRequest.getToken())
                    .build();

            Charge charge = Charge.create(createParams);
            return ResponseEntity.ok("Payment successful: " + charge.getId());
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment failed: " + e.getMessage());
        }
    }
}

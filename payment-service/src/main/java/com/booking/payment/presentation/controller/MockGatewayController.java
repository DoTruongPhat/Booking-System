package com.booking.payment.presentation.controller;

import com.booking.payment.application.port.in.VerifyCallbackUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/mock-gateway")
@RequiredArgsConstructor
@Slf4j
public class MockGatewayController {

    private final VerifyCallbackUseCase verifyCallbackUseCase;

    /**
     * Mock VNPAY payment page.
     * User clicks Success/Fail → triggers callback.
     */
    @GetMapping(value = "/vnpay", produces = MediaType.TEXT_HTML_VALUE)
    public String vnpayPage(
            @RequestParam String txn,
            @RequestParam String amount,
            @RequestParam String bookingId) {

        return """
            <html>
            <head><title>VNPAY Mock Gateway</title>
            <style>
                body { font-family: Arial; max-width: 500px; margin: 50px auto; text-align: center; }
                .card { border: 1px solid #ddd; padding: 30px; border-radius: 8px; }
                h2 { color: #1a56db; }
                .info { margin: 20px 0; text-align: left; }
                .info div { margin: 8px 0; }
                .label { color: #666; }
                .value { font-weight: bold; }
                .btn { padding: 12px 30px; margin: 10px; border: none; border-radius: 6px;
                       cursor: pointer; font-size: 16px; color: white; }
                .btn-success { background: #10b981; }
                .btn-fail { background: #ef4444; }
                .btn:hover { opacity: 0.9; }
            </style></head>
            <body>
                <div class="card">
                    <h2>VNPAY Payment</h2>
                    <div class="info">
                        <div><span class="label">Transaction:</span> <span class="value">%s</span></div>
                        <div><span class="label">Amount:</span> <span class="value">%s VND</span></div>
                        <div><span class="label">Booking:</span> <span class="value">%s</span></div>
                    </div>
                    <form method="POST" action="/mock-gateway/vnpay/process">
                        <input type="hidden" name="txn" value="%s"/>
                        <input type="hidden" name="amount" value="%s"/>
                        <button type="submit" name="action" value="success" class="btn btn-success">Pay Now</button>
                        <button type="submit" name="action" value="fail" class="btn btn-fail">Cancel</button>
                    </form>
                </div>
            </body>
            </html>
            """.formatted(txn, amount, bookingId, txn, amount);
    }

    @PostMapping("/vnpay/process")
    public String vnpayProcess(
            @RequestParam String txn,
            @RequestParam String amount,
            @RequestParam String action) {

        Map<String, String> callbackParams = new HashMap<>();
        callbackParams.put("vnp_TxnRef", txn);
        callbackParams.put("vnp_Amount", amount);
        callbackParams.put("vnp_ResponseCode", "success".equals(action) ? "00" : "24");

        log.info("VNPAY Mock: user clicked {}, triggering callback for txn={}", action, txn);

        try {
            verifyCallbackUseCase.execute("VNPAY", callbackParams);
        } catch (Exception e) {
            log.error("Mock callback error: {}", e.getMessage());
        }

        boolean success = "success".equals(action);
        return """
            <html>
            <head><title>Payment %s</title>
            <style>
                body { font-family: Arial; max-width: 500px; margin: 50px auto; text-align: center; }
                .result { padding: 30px; border-radius: 8px; }
                .success { background: #d1fae5; color: #065f46; }
                .failed { background: #fee2e2; color: #991b1b; }
                a { color: #1a56db; }
            </style></head>
            <body>
                <div class="result %s">
                    <h2>%s</h2>
                    <p>Transaction: %s</p>
                    <p><a href="http://localhost:4200/user/bookings">Back to Bookings</a></p>
                </div>
            </body>
            </html>
            """.formatted(
                success ? "Success" : "Failed",
                success ? "success" : "failed",
                success ? "Payment Successful!" : "Payment Failed!",
                txn
        );
    }

    /**
     * Mock VietQR payment page.
     */
    @GetMapping(value = "/vietqr", produces = MediaType.TEXT_HTML_VALUE)
    public String vietqrPage(
            @RequestParam String txn,
            @RequestParam String amount,
            @RequestParam String bookingId) {

        return """
            <html>
            <head><title>VietQR Mock Gateway</title>
            <style>
                body { font-family: Arial; max-width: 500px; margin: 50px auto; text-align: center; }
                .card { border: 1px solid #ddd; padding: 30px; border-radius: 8px; }
                h2 { color: #e11d48; }
                .qr-placeholder { width: 200px; height: 200px; margin: 20px auto;
                    border: 2px dashed #ccc; display: flex; align-items: center;
                    justify-content: center; color: #999; font-size: 14px; }
                .info div { margin: 8px 0; }
                .label { color: #666; }
                .value { font-weight: bold; }
                .btn { padding: 12px 30px; margin: 10px; border: none; border-radius: 6px;
                       cursor: pointer; font-size: 16px; color: white; }
                .btn-success { background: #10b981; }
                .btn-fail { background: #ef4444; }
            </style></head>
            <body>
                <div class="card">
                    <h2>VietQR Payment</h2>
                    <div class="qr-placeholder">QR Code Here<br/>(Mock)</div>
                    <div class="info">
                        <div><span class="label">Transaction:</span> <span class="value">%s</span></div>
                        <div><span class="label">Amount:</span> <span class="value">%s VND</span></div>
                    </div>
                    <form method="POST" action="/mock-gateway/vietqr/process">
                        <input type="hidden" name="txn" value="%s"/>
                        <input type="hidden" name="amount" value="%s"/>
                        <button type="submit" name="action" value="success" class="btn btn-success">Confirm Paid</button>
                        <button type="submit" name="action" value="fail" class="btn btn-fail">Cancel</button>
                    </form>
                </div>
            </body>
            </html>
            """.formatted(txn, amount, txn, amount);
    }

    @PostMapping("/vietqr/process")
    public String vietqrProcess(
            @RequestParam String txn,
            @RequestParam String amount,
            @RequestParam String action) {

        Map<String, String> callbackParams = new HashMap<>();
        callbackParams.put("txnRef", txn);
        callbackParams.put("amount", amount);
        callbackParams.put("resultCode", "success".equals(action) ? "00" : "99");

        log.info("VietQR Mock: user clicked {}, triggering callback for txn={}", action, txn);

        try {
            verifyCallbackUseCase.execute("VIETQR", callbackParams);
        } catch (Exception e) {
            log.error("Mock callback error: {}", e.getMessage());
        }

        boolean success = "success".equals(action);
        return """
            <html>
            <head><title>Payment %s</title>
            <style>
                body { font-family: Arial; max-width: 500px; margin: 50px auto; text-align: center; }
                .result { padding: 30px; border-radius: 8px; }
                .success { background: #d1fae5; color: #065f46; }
                .failed { background: #fee2e2; color: #991b1b; }
                a { color: #1a56db; }
            </style></head>
            <body>
                <div class="result %s">
                    <h2>%s</h2>
                    <p>Transaction: %s</p>
                    <p><a href="http://localhost:4200/user/bookings">Back to Bookings</a></p>
                </div>
            </body>
            </html>
            """.formatted(
                success ? "Success" : "Failed",
                success ? "success" : "failed",
                success ? "Payment Successful!" : "Payment Failed!",
                txn
        );
    }
}
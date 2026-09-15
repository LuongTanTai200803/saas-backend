package com.saasai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasai.entity.BillingInvoice;
import com.saasai.entity.BillingInvoice.InvoiceStatus;
import com.saasai.feature.payment.BillingController;
import com.saasai.feature.payment.BillingInvoiceDTO;
import com.saasai.feature.payment.BillingService;
import com.saasai.entity.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BillingControllerTest {

    private static final String USER_ID = "user-uuid-10293";

    private static final User USER = User.builder()
            .userId(USER_ID)
            .email("test@example.com")
            .build();

    @Mock
    private BillingService billingService;

    @InjectMocks
    private BillingController billingController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(billingController).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(USER_ID, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createInvoiceShouldReturnInvoice() throws Exception {
        BillingInvoice invoice = BillingInvoice.builder()
                .invoiceId("INV_99823")
                .memoId("NAPTIEN_10293_INV99823")
                .originalAmount(6588000L)
                .discountAmount(1317600L)
                .finalAmount(5270400L)
                .qrCodeUrl("https://img.vietqr.io/image/vietinbank-12345678-qr_only.jpg?amount=5270400&addInfo=NAPTIEN_10293_INV99823")
                .status(InvoiceStatus.PENDING)
                .build();

        when(billingService.createInvoice(USER, "PROFESSIONAL", 12)).thenReturn(invoice);

        mockMvc.perform(post("/api/v1/billing/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "packageType", "PROFESSIONAL",
                                "durationMonths", 12
                        ))))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.invoiceId").value("INV_99823"))
                .andExpect(jsonPath("$.finalAmount").value(5270400))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}

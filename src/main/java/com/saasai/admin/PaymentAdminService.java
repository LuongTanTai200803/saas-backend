package com.saasai.admin;

import com.saasai.admin.InvoiceDTO;
import com.saasai.admin.TransactionDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentAdminService {
    List<InvoiceDTO> listInvoices();
    InvoiceDTO getInvoice(String invoiceId);
    List<TransactionDTO> listTransactions();
    InvoiceDTO regenerateInvoiceQr(String invoiceId);

    List<TransactionDTO> searchTransactions(
            String status,
            String userId,
            LocalDateTime from,
            LocalDateTime to
    );
}
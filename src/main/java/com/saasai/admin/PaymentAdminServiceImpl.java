package com.saasai.admin;

import com.saasai.entity.BillingInvoice;
import com.saasai.entity.TransactionRecord;
import com.saasai.feature.payment.PaymentQrService;
import com.saasai.repository.BillingInvoiceRepository;
import com.saasai.repository.TransactionRecordRepository;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class PaymentAdminServiceImpl implements PaymentAdminService {

    @Autowired
    private BillingInvoiceRepository billingInvoiceRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Autowired
    private PaymentQrService paymentQrService;

    @Override
    public List<InvoiceDTO> listInvoices() {
        return billingInvoiceRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public InvoiceDTO getInvoice(String invoiceId) {
        return billingInvoiceRepository.findById(invoiceId)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    public List<TransactionDTO> listTransactions() {
        return transactionRecordRepository.findAll().stream()
                .map(this::toTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InvoiceDTO regenerateInvoiceQr(String invoiceId) {
        BillingInvoice invoice =
                billingInvoiceRepository.findById(invoiceId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invoice không tồn tại: " + invoiceId
                                ));

        if (invoice.getStatus() == BillingInvoice.InvoiceStatus.PAID) {
            throw new IllegalStateException(
                    "Không thể regenerate QR cho invoice đã thanh toán"
            );
        }

        // if (invoice.getStatus() != BillingInvoice.InvoiceStatus.PENDING) {
        //     throw new IllegalStateException(
        //             "Chỉ được regenerate QR cho invoice đang PENDING"
        //     );
        // }

        if (invoice.getFinalAmount() == null
                || invoice.getFinalAmount() <= 0) {
            throw new IllegalStateException(
                    "Invoice không có số tiền hợp lệ"
            );
        }

    PaymentQrService.QrPayload qr =
            paymentQrService.generate(
                    invoice.getFinalAmount(),
                    invoice.getMemoId()
            );

        invoice.setQrCodeUrl(qr.qrCodeUrl());
        invoice.setQrBankSnapshot(qr.bankSnapshot());

        BillingInvoice saved =
                billingInvoiceRepository.save(invoice);

        return toDto(saved);
    }

    private InvoiceDTO toDto(BillingInvoice i) {
        return new InvoiceDTO(
                i.getInvoiceId(),
                i.getUser() != null ? i.getUser().getUserId() : null,
                i.getAdminPackageConfig() != null ? i.getAdminPackageConfig().getPackageType() : null,
                i.getFinalAmount() != null ? i.getFinalAmount().longValue() : null,
                i.getStatus() != null ? i.getStatus().name() : null,
                i.getMemoId(),
                i.getQrCodeUrl(),
                i.getCreatedAt(),
                i.getPaymentDate()
        );
    }

    private TransactionDTO toTransactionDto(TransactionRecord t) {
        return new TransactionDTO(
                t.getId(),
                t.getInvoiceId(),
                t.getExternalTransactionId(),
                t.getAmount() != null ? t.getAmount().longValue() : null,
                t.getStatus(),
                t.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDTO> searchTransactions(
            String status,
            String userId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return transactionRecordRepository.search(
                        status,
                        userId,
                        from,
                        to
                )
                .stream()
                .map(this::toTransactionDto)
                .toList();
    }
}
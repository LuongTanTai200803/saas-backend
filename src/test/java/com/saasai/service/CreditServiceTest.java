package com.saasai.service;

import com.saasai.dto.CreditEstimateDTO;
import com.saasai.dto.CreditEstimateResponseDTO;
import com.saasai.dto.CreditSettleRequestDTO;
import com.saasai.entity.CreditTransaction;
import com.saasai.entity.FileMetadata;
import com.saasai.entity.User;
import com.saasai.feature.payment.CreditAccount;
import com.saasai.feature.payment.CreditAccountRepository;
import com.saasai.repository.CreditTransactionRepository;
import com.saasai.repository.FileMetadataRepository;
import com.saasai.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    private static final String USER_ID =
            "10293847-1029-4038-8047-102938471029";

    private static final String OTHER_USER_ID =
            "20384958-2038-4039-8058-203849582038";

    private static final String USER_EMAIL =
            "user@example.com";

    private static final String FILE_ID =
            "10293847-1029-4038-8047-102938471030";

    private static final Integer SESSION_ID = null;

    @Mock
    private CreditTransactionRepository creditTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileMetadataRepository fileUploadRepository;

    @Mock
    private CreditAccountRepository creditAccountRepository;

    @InjectMocks
    private CreditService creditService;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(USER_ID, null);

        authentication.setDetails(USER_EMAIL);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void estimateCreditsShouldUseCreditAccountBalance() {
        User user = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .build();

        CreditAccount account = CreditAccount.builder()
                .userId(USER_ID)
                .user(user)
                .monthlyQuotaRemaining(8.0)
                .purchasedCreditBalance(2.0)
                .build();

        FileMetadata file = FileMetadata.builder()
                .fileId(FILE_ID)
                .user(user)
                .fileSize(0L)
                .build();

        CreditEstimateDTO request = CreditEstimateDTO.builder()
                .modelName("claude-sonnet-4.6")
                .features(List.of("LEGAL_REVIEW", "EXPORT_DOCX"))
                .fileId(FILE_ID)
                .build();

        when(userRepository.findByEmail(USER_EMAIL))
                .thenReturn(Optional.of(user));
        when(creditAccountRepository.findById(USER_ID))
                .thenReturn(Optional.of(account));
        when(fileUploadRepository.findById(FILE_ID))
                .thenReturn(Optional.of(file));

        CreditEstimateResponseDTO response =
                creditService.estimateCredits(request);

        // Model: 4.0
        // Features: 2 * 0.75 = 1.5
        // File: minimum 0.5
        assertEquals(6.0, response.getEstimatedCredits());
        assertEquals(10.0, response.getCurrentCredits());
        assertTrue(response.getIsEligible());
    }

    @Test
    void estimateCreditsShouldUseMonthlyAndPurchasedTogether() {
        User user = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .build();

        CreditAccount account = CreditAccount.builder()
                .userId(USER_ID)
                .user(user)
                .monthlyQuotaRemaining(2.0)
                .purchasedCreditBalance(1.0)
                .build();

        FileMetadata file = FileMetadata.builder()
                .fileId(FILE_ID)
                .user(user)
                .fileSize(0L)
                .build();

        CreditEstimateDTO request = CreditEstimateDTO.builder()
                .modelName("claude-sonnet-4.6")
                .features(List.of("LEGAL_REVIEW"))
                .fileId(FILE_ID)
                .build();

        when(userRepository.findByEmail(USER_EMAIL))
                .thenReturn(Optional.of(user));
        when(creditAccountRepository.findById(USER_ID))
                .thenReturn(Optional.of(account));
        when(fileUploadRepository.findById(FILE_ID))
                .thenReturn(Optional.of(file));

        CreditEstimateResponseDTO response =
                creditService.estimateCredits(request);

        assertEquals(3.0, response.getCurrentCredits());
        assertFalse(response.getIsEligible());
    }

    @Test
    void estimateCreditsShouldTreatNegativeBalancesAsZero() {
        User user = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .build();

        CreditAccount account = CreditAccount.builder()
                .userId(USER_ID)
                .user(user)
                .monthlyQuotaRemaining(-2.0)
                .purchasedCreditBalance(5.0)
                .build();

        FileMetadata file = FileMetadata.builder()
                .fileId(FILE_ID)
                .user(user)
                .fileSize(0L)
                .build();

        CreditEstimateDTO request = CreditEstimateDTO.builder()
                .modelName("claude-sonnet-4.6")
                .features(List.of("LEGAL_REVIEW"))
                .fileId(FILE_ID)
                .build();

        when(userRepository.findByEmail(USER_EMAIL))
                .thenReturn(Optional.of(user));
        when(creditAccountRepository.findById(USER_ID))
                .thenReturn(Optional.of(account));
        when(fileUploadRepository.findById(FILE_ID))
                .thenReturn(Optional.of(file));

        CreditEstimateResponseDTO response =
                creditService.estimateCredits(request);

        assertEquals(5.0, response.getCurrentCredits());
    }

    @Test
    void estimateCreditsShouldRejectFileOwnedByAnotherUser() {
        User user = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .build();

        User otherUser = User.builder()
                .userId(OTHER_USER_ID)
                .email("other@example.com")
                .build();

        CreditAccount account = CreditAccount.builder()
                .userId(USER_ID)
                .user(user)
                .monthlyQuotaRemaining(20.0)
                .purchasedCreditBalance(0.0)
                .build();

        FileMetadata file = FileMetadata.builder()
                .fileId(FILE_ID)
                .user(otherUser)
                .fileSize(1024L)
                .build();

        CreditEstimateDTO request = CreditEstimateDTO.builder()
                .modelName("claude-sonnet-4.6")
                .features(List.of("LEGAL_REVIEW"))
                .fileId(FILE_ID)
                .build();

        when(userRepository.findByEmail(USER_EMAIL))
                .thenReturn(Optional.of(user));
        when(creditAccountRepository.findById(USER_ID))
                .thenReturn(Optional.of(account));
        when(fileUploadRepository.findById(FILE_ID))
                .thenReturn(Optional.of(file));

        assertThrows(
                AccessDeniedException.class,
                () -> creditService.estimateCredits(request)
        );
    }
    @Test
    void recordHoldShouldUseMonthlyBeforePurchased() {
        User user = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .build();

        CreditAccount account = CreditAccount.builder()
                .userId(USER_ID)
                .user(user)
                .monthlyQuotaRemaining(3.0)
                .purchasedCreditBalance(10.0)
                .build();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(creditAccountRepository.findById(USER_ID))
                .thenReturn(Optional.of(account));
        when(creditTransactionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreditSettleRequestDTO request = new CreditSettleRequestDTO();
        request.setEstimatedHold(5.0);
        request.setDescription("AI HOLD");

        CreditTransaction transaction =
                creditService.recordHoldTransactionWithSnapshot(
                        USER_ID,
                        SESSION_ID,
                        request
                );

        assertEquals(0.0, account.getMonthlyQuotaRemaining());
        assertEquals(8.0, account.getPurchasedCreditBalance());
        assertEquals(5.0, transaction.getTotalCreditHold());
        assertTrue(transaction.getDescription()
                .contains("\"monthlyDeducted\":3.0"));
        assertTrue(transaction.getDescription()
                .contains("\"purchasedDeducted\":2.0"));
    }

    @Test
void recordHoldShouldUsePurchasedAfterNegativeMonthlyBalance() {
    User user = User.builder()
            .userId(USER_ID)
            .email(USER_EMAIL)
            .build();

    CreditAccount account = CreditAccount.builder()
            .userId(USER_ID)
            .user(user)
            .monthlyQuotaRemaining(-2.0)
            .purchasedCreditBalance(10.0)
            .build();

    when(userRepository.findById(USER_ID))
            .thenReturn(Optional.of(user));
    when(creditAccountRepository.findById(USER_ID))
            .thenReturn(Optional.of(account));
    when(creditTransactionRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

    CreditSettleRequestDTO request = new CreditSettleRequestDTO();
    request.setEstimatedHold(5.0);

    CreditTransaction transaction =
            creditService.recordHoldTransactionWithSnapshot(
                    USER_ID,
                    SESSION_ID,
                    request
            );

    assertEquals(0.0, account.getMonthlyQuotaRemaining());
    assertEquals(3.0, account.getPurchasedCreditBalance());
    assertEquals(7.0, transaction.getTotalCreditHold());
    assertTrue(transaction.getDescription()
            .contains("\"monthlyDeducted\":0.0"));
    assertTrue(transaction.getDescription()
            .contains("\"purchasedDeducted\":7.0"));
}

}
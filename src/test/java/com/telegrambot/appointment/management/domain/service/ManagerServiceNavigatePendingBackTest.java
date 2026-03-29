package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.user.manager.ManagerAction;
import com.telegrambot.appointment.management.domain.model.user.manager.ManagerPendingAction;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.ServiceRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.context.ManagerPendingActionRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistWhitelistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerServiceNavigatePendingBackTest {

    private static final long TELEGRAM_ID = 9001L;
    private static final long CHAT_ID = 70001L;

    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private ManagerPendingActionRepository pendingActionRepository;
    @Mock
    private SpecialistWhitelistRepository specialistWhitelistRepository;
    @Mock
    private SpecialistRepository specialistRepository;
    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private ManagerService managerService;

    @Test
    void noPendingYieldsEmptyAndDoesNotTouchRepository() {
        when(pendingActionRepository.findById(TELEGRAM_ID)).thenReturn(Optional.empty());

        Optional<SendMessage> result = managerService.navigatePendingBack(TELEGRAM_ID, CHAT_ID);

        assertTrue(result.isEmpty());
        verify(pendingActionRepository).findById(TELEGRAM_ID);
        verifyNoMoreInteractions(pendingActionRepository);
    }

    @Test
    void backFromFirstServiceStepClearsPendingAndReturnsEmpty() {
        ManagerPendingAction pending = new ManagerPendingAction(TELEGRAM_ID, ManagerAction.AWAITING_SERVICE_NAME);
        when(pendingActionRepository.findById(TELEGRAM_ID)).thenReturn(Optional.of(pending));
        when(pendingActionRepository.existsById(TELEGRAM_ID)).thenReturn(true);

        Optional<SendMessage> result = managerService.navigatePendingBack(TELEGRAM_ID, CHAT_ID);

        assertTrue(result.isEmpty());
        verify(pendingActionRepository).deleteById(TELEGRAM_ID);
    }

    @Test
    void backFromWhitelistStepClearsPendingAndReturnsEmpty() {
        ManagerPendingAction pending = new ManagerPendingAction(TELEGRAM_ID, ManagerAction.AWAITING_SPECIALIST_USERNAME);
        when(pendingActionRepository.findById(TELEGRAM_ID)).thenReturn(Optional.of(pending));
        when(pendingActionRepository.existsById(TELEGRAM_ID)).thenReturn(true);

        Optional<SendMessage> result = managerService.navigatePendingBack(TELEGRAM_ID, CHAT_ID);

        assertTrue(result.isEmpty());
        verify(pendingActionRepository).deleteById(TELEGRAM_ID);
    }

    @Test
    void backFromPriceGoesToNameAndClearsDraftPrice() {
        ManagerPendingAction pending = new ManagerPendingAction(TELEGRAM_ID, ManagerAction.AWAITING_SERVICE_PRICE);
        pending.setDraftServicePrice("99.00");
        when(pendingActionRepository.findById(TELEGRAM_ID)).thenReturn(Optional.of(pending));

        Optional<SendMessage> result = managerService.navigatePendingBack(TELEGRAM_ID, CHAT_ID);

        assertTrue(result.isPresent());
        ArgumentCaptor<ManagerPendingAction> captor = ArgumentCaptor.forClass(ManagerPendingAction.class);
        verify(pendingActionRepository).save(captor.capture());
        ManagerPendingAction saved = captor.getValue();
        assertEquals(ManagerAction.AWAITING_SERVICE_NAME, saved.getAction());
        assertEquals(null, saved.getDraftServicePrice());

        SendMessage message = result.orElseThrow();
        assertEquals(String.valueOf(CHAT_ID), message.getChatId());
        assertTrue(message.getText().contains("название"));
        assertCallbackDataPresent(message, "MANAGER_PENDING_BACK");
        assertCallbackDataPresent(message, "MANAGER_MAIN_MENU");
    }

    @Test
    void backFromDurationGoesToPriceAndClearsDraftDuration() {
        ManagerPendingAction pending = new ManagerPendingAction(TELEGRAM_ID, ManagerAction.AWAITING_SERVICE_DURATION);
        pending.setDraftServiceDurationMinutes("45");
        when(pendingActionRepository.findById(TELEGRAM_ID)).thenReturn(Optional.of(pending));

        Optional<SendMessage> result = managerService.navigatePendingBack(TELEGRAM_ID, CHAT_ID);

        assertTrue(result.isPresent());
        ArgumentCaptor<ManagerPendingAction> captor = ArgumentCaptor.forClass(ManagerPendingAction.class);
        verify(pendingActionRepository).save(captor.capture());
        ManagerPendingAction saved = captor.getValue();
        assertEquals(ManagerAction.AWAITING_SERVICE_PRICE, saved.getAction());
        assertEquals(null, saved.getDraftServiceDurationMinutes());

        SendMessage message = result.orElseThrow();
        assertTrue(message.getText().contains("цену"));
        assertCallbackDataPresent(message, "MANAGER_PENDING_BACK");
    }

    private static void assertCallbackDataPresent(SendMessage message, String callbackData) {
        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) message.getReplyMarkup();
        List<List<InlineKeyboardButton>> rows = markup.getKeyboard();
        boolean found = rows.stream()
                .flatMap(List::stream)
                .map(InlineKeyboardButton::getCallbackData)
                .anyMatch(callbackData::equals);
        assertTrue(found, "Expected callback " + callbackData);
    }
}

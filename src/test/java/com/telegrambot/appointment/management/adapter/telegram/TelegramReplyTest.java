package com.telegrambot.appointment.management.adapter.telegram;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramReplyTest {

    @Test
    void sendOrEditWithMessageIdExecutesEditMessageText() {
        List<BotApiMethod<?>> executed = new ArrayList<>();
        TelegramReply reply = new TelegramReply(executed::add);

        SendMessage outgoing = new SendMessage("42", "hello");
        outgoing.setParseMode("Markdown");
        outgoing.setReplyMarkup(new InlineKeyboardMarkup(List.of()));

        reply.sendOrEdit(outgoing, 7);

        assertEquals(1, executed.size());
        EditMessageText edit = assertInstanceOf(EditMessageText.class, executed.get(0));
        assertEquals("42", edit.getChatId());
        assertEquals(7, edit.getMessageId());
        assertEquals("hello", edit.getText());
        assertEquals("Markdown", edit.getParseMode());
    }

    @Test
    void sendOrEditWithoutMessageIdExecutesSendMessage() {
        List<BotApiMethod<?>> executed = new ArrayList<>();
        TelegramReply reply = new TelegramReply(executed::add);

        SendMessage outgoing = new SendMessage("1", "x");
        reply.sendOrEdit(outgoing, null);

        assertEquals(1, executed.size());
        assertEquals(outgoing, executed.get(0));
    }

    @Test
    void sendOrEditClearsInlineKeyboardWhenSendMessageHasNoMarkup() {
        List<BotApiMethod<?>> executed = new ArrayList<>();
        TelegramReply reply = new TelegramReply(executed::add);

        SendMessage outgoing = new SendMessage("9", "done");
        reply.sendOrEdit(outgoing, 3);

        EditMessageText edit = assertInstanceOf(EditMessageText.class, executed.get(0));
        assertTrue(edit.getReplyMarkup().getKeyboard().isEmpty());
    }

    @Test
    void deleteMessageExecutesDeleteMessageWithChatAndId() {
        List<BotApiMethod<?>> executed = new ArrayList<>();
        TelegramReply reply = new TelegramReply(executed::add);

        reply.deleteMessage("100", 55);

        assertEquals(1, executed.size());
        DeleteMessage del = assertInstanceOf(DeleteMessage.class, executed.get(0));
        assertEquals("100", del.getChatId());
        assertEquals(55, del.getMessageId());
    }

    @Test
    void deleteMessageSkippedWhenMessageIdNull() {
        List<BotApiMethod<?>> executed = new ArrayList<>();
        TelegramReply reply = new TelegramReply(executed::add);

        reply.deleteMessage("1", null);

        assertTrue(executed.isEmpty());
    }
}

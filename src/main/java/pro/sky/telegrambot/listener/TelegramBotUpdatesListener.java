package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.entity.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;


    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            if (update.message().text().equals("/start")) {
                sendMessageChat(update.message().chat().id(),
                        "Привет, " + update.message().from().firstName() + " " + update.message().from().lastName() +
                                "! Этот бот предназначен для напоминания важных дел. Введите сообщение в формате:\n 01.01.2023 20:00 ТЕКСТ СООБЩЕНИЯ");
            } else {
                 addMessageTask(update);
                }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    // Sending message to chat
    private void sendMessageChat(Long chatId, String messageText) {
        SendMessage message = new SendMessage(chatId, messageText);
        SendResponse response = telegramBot.execute(message);
    }

    //Add and validate a message
    private void addMessageTask(Update update) {
        String task = update.message().text();
        Long chatId = update.message().chat().id();
        String time;
        String message;

        Pattern pattern = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");
        Matcher matcher = pattern.matcher(task);

        if (matcher.matches()) {
            time = matcher.group(1);
            message = matcher.group(3);
        } else {
            sendMessageChat(chatId, "НЕВЕРНЫЙ ФОРМАТ ВВОДА!\n " +
                    "ВВЕДИТЕ: 01.01.2023 20:00 ТЕКСТ СООБЩЕНИЯ");
            return;
        }
        NotificationTask nTask = new NotificationTask();
        nTask.setChatId(chatId);
        nTask.setTime(LocalDateTime.parse(time, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        nTask.setMessage(message);

        notificationTaskRepository.save(nTask);

        sendMessageChat(chatId, "Готово! Я напомню вам об этом! " + time);
        }

    //This method provides the user with a reminder message
        @Scheduled(cron = "0 0/1 * * * *")
    public void checkNotification() {
            LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
            List<NotificationTask> nTask = notificationTaskRepository.findTaskByTime(time);
            if (!nTask.isEmpty()) {
                nTask.forEach(notificationTask -> sendMessageChat(notificationTask.getChatId(), "Напоминанаю: " + notificationTask.getMessage()));
                notificationTaskRepository.delete((NotificationTask) nTask);

                }
            }
        }


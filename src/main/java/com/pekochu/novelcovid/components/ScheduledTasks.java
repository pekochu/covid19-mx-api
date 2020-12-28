package com.pekochu.novelcovid.components;

import com.pekochu.novelcovid.service.covid19.BotTwitter;
import com.pekochu.novelcovid.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ScheduledTasks {

    @Autowired
    BotTwitter twitter;

    private final static Logger LOGGER = LoggerFactory.getLogger(ScheduledTasks.class.getCanonicalName());
    private final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Scheduled(cron = "0 0 22 * * ?")
    public void scheduledCovidMexico() {
        LOGGER.info("Cron Task :: {} - Posting tweet about Covid-19 in Mexico",
                dateTimeFormatter.format(LocalDateTime.now()));

        Utils.postCovid19image(twitter);
    }

}

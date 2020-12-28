package com.pekochu.novelcovid;

import com.pekochu.novelcovid.service.covid19.CovidSummaryGraphics;
import com.pekochu.novelcovid.service.covid19.CovidSummaryProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTest {

    @Autowired
    CovidSummaryProvider covidProvider;

    @Autowired
    CovidSummaryGraphics covidSummaryGraphics;

    private final static Logger LOGGER = LoggerFactory.getLogger(ApplicationTest.class.getCanonicalName());

    @Test
    public void contextLoads() {
        // LOGGER.info("JSON SINAVE: {}", covidProvider.getCovidMX().toString(2));
        // covidProvider.downloadOpenData();
        covidProvider.updateData();
        // covidSummaryGraphics.createImageLatest();
    }
}

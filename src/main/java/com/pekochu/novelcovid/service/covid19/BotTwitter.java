package com.pekochu.novelcovid.service.covid19;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import java.io.File;

@Service
public class BotTwitter {

    private final static Logger LOGGER = LoggerFactory.getLogger(BotTwitter.class.getCanonicalName());

    public BotTwitter(){
        // ---------------------
    }

    public void testTweet(String tweet){
        Twitter twitter = TwitterFactory.getSingleton();
        try{
            StatusUpdate status = new StatusUpdate(tweet);

            twitter.updateStatus(status);

            LOGGER.info("Tweet publicado exitosamente");
        }catch(TwitterException e){
            LOGGER.error(e.getMessage());
        }
    }

    public void tweetImage(String tweet, File image){
        Twitter twitter = TwitterFactory.getSingleton();
        try{
            StatusUpdate status = new StatusUpdate(tweet);

            status.setMedia(image);
            twitter.updateStatus(status);

            LOGGER.info("Tweet publicado exitosamente");
        }catch(TwitterException e){
            LOGGER.error(e.getMessage());
        }
    }
}

package com.pekochu.novelcovid.util;

import com.pekochu.novelcovid.service.covid19.BotTwitter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Utils {

    public static void postCovid19image(BotTwitter twitter){
        StringBuilder postInfo = new StringBuilder();
        postInfo.append("Información sobre el #COVID_19 en México. Cifras nacionales y por estado.\n\n");
        postInfo.append("Soy un #bot y puedo cometer errores. Publicación meramente informativa.");
        postInfo.append(" #QuédateEnCasa #SalvaVidas");

        // twitter.tweetImage(postInfo.toString(), new CovidSummaryProvider().createImage());
    }

    public static StringBuilder discordSetMulticraftErrors(JSONArray arg){
        StringBuilder description = new StringBuilder();

        for(Object o : arg.toList()){
            if(o instanceof String){
                description.append((String)o);
                description.append("\n");
            }
        }

        return description;
    }

    public static InetAddress getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            // edited, to include @Arthur's comment
            // e.g. in case JSONArray is valid as well...
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

}

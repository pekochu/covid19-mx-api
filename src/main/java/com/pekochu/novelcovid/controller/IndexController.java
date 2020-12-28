package com.pekochu.novelcovid.controller;

import io.swagger.annotations.Api;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
@Api(value = "index")
public class IndexController {

    private final static Logger LOGGER = LoggerFactory.getLogger(IndexController.class.getCanonicalName());

    @RequestMapping(value = {"/", "/**"})
    public @ResponseBody
    ResponseEntity<String> main() {
        LOGGER.info("index");
        JSONObject index = new JSONObject();

        index.put("source", "https://github.com/pekochu/covid19-mx-api");
        index.put("api", "/api/v1/covid/");

        return ResponseEntity.status(HttpStatus.OK.value()).body(index.toString());
    }

}

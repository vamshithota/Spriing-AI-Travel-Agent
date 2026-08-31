package com.company.openai.tools;

import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class TimeTools {

    @Tool(name="getCurrentLocalTime", description = "Get the current time in the user's timezone")
    String getCurrentLocalTime() {
            return LocalTime.now().toString();
    }
    @Tool(name="getCurrentTime", description = "Get the current time in the specified time zone.")
    public String getCurrentTime(@ToolParam(description = "Value representing the time zone") String timeZone){
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("hh:mm:ss a");
        return LocalTime.now(ZoneId.of(timeZone))
                .format(formatter);
    }


}

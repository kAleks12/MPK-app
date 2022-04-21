package com.example.app;

import java.util.ArrayList;

public class JSonParser {

    static public ArrayList<String> parseVehicles(String jsonString){
        ArrayList<String> returnData = new ArrayList<>();

        jsonString = jsonString.replace("]", "");
        jsonString = jsonString.replace("[", "");
        jsonString = jsonString.replace("},", "}!");

        String[] vehicles = jsonString.split("!");

        for(String vehicle: vehicles){
            vehicle = vehicle.replace("{", "");
            vehicle = vehicle.replace("}", "");

            String[] attributes = vehicle.split(",");

            for(String attribute: attributes){
                String[] splitAttr = attribute.split(":");
                int index = 0;

                for(String value: splitAttr){
                    value = value.replace("\"", "");

                    if(index%2 != 0)
                        returnData.add(value);
                    index++;
                }

            }
        }
        return returnData;
    }
}
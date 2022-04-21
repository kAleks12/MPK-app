package com.example.app;

import java.util.ArrayList;
import java.util.LinkedList;

public class Vehicle {
    String line;
    int id;
    double x;
    double y;
    String type;

    Vehicle( String line, String type, double y, double x,int id){
        this.line = line;
        this.type = type;
        this.id = id;
        this.x = x;
        this.y = y;

    }

    public String getLine(){ return line; }

    static LinkedList<Vehicle> createVehicles (ArrayList<String> attributes){
        LinkedList<Vehicle> vehicles = new LinkedList<>();
        int index = 0;
        String currLine = "";
        String currType= "";
        double currX = 0;
        double currY = 0;
        int currId = 0;


        for(String attribute : attributes){
            switch(index%5){
                case 0 -> currLine = attribute;
                case 1 -> currType = attribute;
                case 2 -> currX = Double.parseDouble(attribute);
                case 3 -> currY = Double.parseDouble(attribute);
                case 4 -> currId = Integer.parseInt(attribute);
            }
            if(++index%5 == 0)
                vehicles.add(new Vehicle(currLine,currType,currY,currX, currId));
        }
        return vehicles;
    }

    public String getType(){
        return type;
    }
}
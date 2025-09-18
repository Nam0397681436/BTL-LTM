/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class JsonUtil {
    private static final Gson G = new Gson();
    public static String toJson(Object o){ return G.toJson(o); }
    public static <T> T fromJson(String s, Class<T> c){ return G.fromJson(s, c); }
    public static JsonObject obj(){ return new JsonObject(); }
}

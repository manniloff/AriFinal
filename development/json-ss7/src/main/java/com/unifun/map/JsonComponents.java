/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.map;

import java.io.Serializable;
import java.util.ArrayList;
import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 *
 * @author okulikov
 */
public class JsonComponents implements Serializable {

    private final ArrayList<JsonComponent> components = new ArrayList();

    public JsonComponents() {
    }

    public JsonComponents(JsonArray objs) {
        for (int i = 0; i < objs.size(); i++) {
            components.add(new JsonComponent((JsonObject) objs.get(i)));
        }
    }

    public JsonComponent get(int i) {
        return components.get(i);
    }

    public void add(JsonComponent c) {
        components.add(c);
    }

    public int size() {
        return components.size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(components.get(i).toString());
        }
        builder.append("]");

        return builder.toString();
    }

}

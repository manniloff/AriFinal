/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.map;

import javax.json.JsonObject;

/**
 *
 * @author okulikov
 */
public class JsonDataCodingScheme {

    private String language;
    private String codingGroup;

    public JsonDataCodingScheme() {
    }

    public JsonDataCodingScheme(JsonObject obj) {
        language = obj.getString("language");
        codingGroup = obj.getString("coding-group");
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCodingGroup() {
        return codingGroup;
    }

    public void setCodingGroup(String codingGroup) {
        this.codingGroup = codingGroup;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");

        builder.append("\"language\"");
        builder.append(":");
        builder.append('"');
        builder.append(language);
        builder.append('"');

        builder.append(",");
        builder.append("\"coding-group\"");
        builder.append(":");
        builder.append('"');
        builder.append(codingGroup);
        builder.append('"');
        builder.append("}");

        return builder.toString();
    }
}

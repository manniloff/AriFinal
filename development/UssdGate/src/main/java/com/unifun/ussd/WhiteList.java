/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 *
 * @author okulikov
 */
public class WhiteList {

    private final ArrayList<String> list = new ArrayList();
    private final static Logger LOGGER = Logger.getLogger(WhiteList.class);
    
    public WhiteList() {
        LOGGER.info("Loading white list");
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream("/opt/unifun/white-list.txt")));
            String entry;
            while ((entry = reader.readLine()) != null) {                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Loading entry " + entry);
                }
                
                entry = entry.trim();
                
                if (entry.length() > 0) {
                    list.add(entry);
                }
            }
            LOGGER.info("Loaded " + list.size() + " entries");
        } catch (Exception e) {
            LOGGER.info("White list is empty");
        }
    }

    public boolean isAllowed(String entry) {
        return list.isEmpty() || list.contains(entry);
    }
}

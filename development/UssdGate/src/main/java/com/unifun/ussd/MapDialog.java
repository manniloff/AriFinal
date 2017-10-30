/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd;

import com.unifun.map.JsonTcapDialog;
import com.unifun.ussd.router.Route;
import com.unifun.ussd.context.ExecutionContext;

/**
 *
 * @author okulikov
 */
public class MapDialog {
    private JsonTcapDialog dialog;
    private Route route;
    private ExecutionContext context;
    
    public MapDialog(JsonTcapDialog dialog) {
        this.dialog = dialog;
    }

    public JsonTcapDialog getDialog() {
        return dialog;
    }

    public void setDialog(JsonTcapDialog dialog) {
        this.dialog = dialog;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public ExecutionContext getContext() {
        return context;
    }

    public void setContext(ExecutionContext context) {
        this.context = context;
    }
    
    
}

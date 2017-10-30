/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd.context;

import com.unifun.ussd.UssMessage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.http.concurrent.FutureCallback;

/**
 *
 * @author okulikov
 */
public interface ExecutionContext extends FutureCallback<UssMessage> {
    public final static ExecutorService EXECUTOR = Executors.newCachedThreadPool();
}

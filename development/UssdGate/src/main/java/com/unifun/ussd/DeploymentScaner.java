/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unifun.ussd;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 *
 * @author okulikov
 */
public class DeploymentScaner implements Runnable {

    private final ArrayList<Deployment> deployments = new ArrayList();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Future future;

    private final Logger LOGGER = Logger.getLogger(DeploymentScaner.class);

    public void add(Deployment d) {
        synchronized (deployments) {
            deployments.add(d);
        }
    }

    public void remove(Deployment d) {
        synchronized (deployments) {
            deployments.remove(d);
        }
    }

    /**
     * Starts router.
     */
    public void start() {
        future = scheduler.scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
        LOGGER.info("Started scanner");
    }

    /**
     * Terminates Router.
     */
    public void stop() {
        if (future != null) {
            future.cancel(true);
        }
        LOGGER.info("Scaner has been stopped");
    }

    @Override
    public void run() {
        LOGGER.info("deployements size: " + deployments.size());
        synchronized (deployments) {
            try {
            deployments.stream().filter((deployment) -> (deployment.isModified())).forEach((deployment) -> {
                try {
                    LOGGER.info("Deploing " + deployment);
                    deployment.reload();
                } catch (Exception e) {
                    LOGGER.warn("Could not re-deploy " + deployment, e);
                }
            });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

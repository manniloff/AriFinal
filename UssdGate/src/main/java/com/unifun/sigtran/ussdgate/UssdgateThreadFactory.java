/**
 * 
 */
package com.unifun.sigtran.ussdgate;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author rbabin
 *
 */
public class UssdgateThreadFactory implements ThreadFactory {
	   private static final AtomicInteger poolNumber = new AtomicInteger(1);
	    private final ThreadGroup group;
	    private final AtomicInteger threadNumber = new AtomicInteger(1);
	    private final String namePrefix;
	    private String poolName;

	    public UssdgateThreadFactory(String poolname) {
	    	this.poolName = poolname;
	        SecurityManager s = System.getSecurityManager();
	        group = (s != null) ? s.getThreadGroup() :
	                              Thread.currentThread().getThreadGroup();
	        namePrefix = poolName+"-pool-" +
	                      poolNumber.getAndIncrement() +
	                     "-thread-";
	    }

	    public Thread newThread(Runnable r) {
	        Thread t = new Thread(group, r,
	                              namePrefix + threadNumber.getAndIncrement(),
	                              0);
	        if (t.isDaemon())
	            t.setDaemon(false);
	        if (t.getPriority() != Thread.NORM_PRIORITY)
	            t.setPriority(Thread.NORM_PRIORITY);
	        return t;
	    }
}

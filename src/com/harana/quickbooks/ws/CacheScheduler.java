package com.harana.quickbooks.ws;

import java.util.Enumeration;
import java.util.HashMap;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class CacheScheduler {
	
	static Scheduler scheduler;
	static HashMap<String, CacheDef> cacheDefs = new HashMap<String, CacheDef>();;
	
	public static void scheduleAutomaticCacheFill(){
		Enumeration<Object> e = PropertiesManager.getPropertiesKeys();
		while (e.hasMoreElements()){
			String key = (String) e.nextElement();
			if (key.startsWith("cache.")){
				String cache = key.substring(key.indexOf(".")+1);
				CacheDef cacheDef;
				if (!cacheDefs.containsKey(cache)){
					cacheDef = new CacheDef();
					String values[] = PropertiesManager.getProperty(key).split(";");
					cacheDef.uploadOperations = values[0];
					cacheDef.chronExp = values[1];
			    	cacheDef.jobDetail = JobBuilder.newJob(ReloadEntitiesJob.class)
			    			.withIdentity(cache+"job", "group1").build();
			    	cacheDef.jobDetail.getJobDataMap().put("uploadOperations", cacheDef.uploadOperations);
			    	cacheDef.jobDetail.getJobDataMap().put("cache", cache);
			    	cacheDefs.put(cache, cacheDef);
				}else{
					cacheDef  = cacheDefs.get(cache);
					try {
						cacheDef.scheduler.shutdown();
						System.out.println("Cache " + cache + "  shutdown");
					} catch (SchedulerException e1) {
						e1.printStackTrace();
					}
				}

				try {
					cacheDef.scheduler = new StdSchedulerFactory().getScheduler();
				} catch (SchedulerException e2) {
					e2.printStackTrace();
				}
				
				
				Trigger trigger = TriggerBuilder
						.newTrigger()
						.withIdentity(cache, "caches")
						.withSchedule(
							CronScheduleBuilder.cronSchedule(cacheDef.chronExp))
						.build();
		    	try {

		    		cacheDef.scheduler.start();
			    	
			    	System.out.println(cache+"job" + " programmated to run");
			    	cacheDef.scheduler.deleteJob(cacheDef.jobDetail.getKey());
			    	cacheDef.scheduler.scheduleJob(cacheDef.jobDetail, trigger);
			    	cacheDef.scheduler.triggerJob(cacheDef.jobDetail.getKey());
				} catch (SchedulerException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	public static void fillCacheNow(String cache){
		CacheDef cacheDef  = cacheDefs.get(cache);
		try {
			cacheDef.scheduler.triggerJob(cacheDef.jobDetail.getKey());
		} catch (SchedulerException e) {
			e.printStackTrace();
		}

	}

	
}
	
class CacheDef{
	public String name;
	public Scheduler scheduler;
	public String chronExp;
	public JobDetail jobDetail;
	public String uploadOperations;
	
}

	

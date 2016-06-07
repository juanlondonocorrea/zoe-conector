package com.harana.quickbooks.ws;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ReloadEntitiesJob implements Job {

	String uploadOperations;
	String cache;

	@Override
	public void execute(JobExecutionContext execContext) throws JobExecutionException {
		uploadOperations = (String) execContext.getJobDetail().getJobDataMap().get("uploadOperations");
		cache = (String) execContext.getJobDetail().getJobDataMap().get("cache");
		exec();
	}
	
	public void exec(){
		System.out.println("running job cache " + cache + " operations " + uploadOperations);
		SynchronizerService ss = new SynchronizerService();
		MessageManager.getInstance().clearCache(cache);
		ss.synch(uploadOperations, "XML", cache, "",null);
		System.out.println("job finish " + cache);
	}

}

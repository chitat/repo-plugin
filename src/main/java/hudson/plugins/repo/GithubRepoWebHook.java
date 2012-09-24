/*
 * The MIT License
 *
 * Copyright (c) 2010, Brad Larson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.repo;

import static java.util.logging.Level.WARNING;

import java.io.File;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Timer;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import hudson.security.ACL;
import hudson.triggers.Trigger;
import hudson.util.AdaptedIterator;
import hudson.util.Iterators.FilterIterator;

import org.kohsuke.stapler.StaplerRequest;
import net.sf.json.JSONObject;

@Extension
public class GithubRepoWebHook implements UnprotectedRootAction {
	
    private static final Pattern REPOSITORY_NAME_PATTERN = Pattern.compile("https?://([^/]+)/([^/]+)/([^/]+)");
    static Timer pushStableTimer = new Timer();
    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return "github-repo-webhook";
    }
    
    /**
     * 1 push to 2 branches will result in 2 pushes.
     */
    public void doIndex(StaplerRequest req) {
        processGitHubPayload(req.getParameter("payload"));
    }
    
    public void processGitHubPayload(String payload) {
    	
    	JSONObject o = JSONObject.fromObject(payload);
    	LOGGER.info("Received Payload:" + o.toString());
    	JSONObject pull_request = o.getJSONObject("pull_request");
    	if ( !pull_request.isNullObject()) {
    		processPullRequest(o, RepoPullRequestTrigger.class);
    	} else {
    		String push = o.getString("after");
    		if ( push != null ) {
    			processPush(o, RepoPushTrigger.class);
    		}
    	}
    }
    public void processPullRequest(final JSONObject o, Class<? extends Trigger<?>> triggerClass)
    {
    	String action 			= o.getString("action");
    	if (action.equals("closed")) {
    		return;
    	}
    	JSONObject pull_request 	= o.getJSONObject("pull_request");
    	JSONObject repository 		= o.getJSONObject("repository");

    	JSONObject head				= pull_request.getJSONObject("head");
    	JSONObject base				= pull_request.getJSONObject("base");

    	final String pull_branch 	= head.getString("ref");
    	final String pusherName 	= head.getJSONObject("user").getString("login");
    	final String base_branch 	= base.getString("ref");
    	
    	final String repoUrl 		= repository.getString("html_url"); // something like 'https://github.com/kohsuke/foo'
    	final String repoName 		= repository.getString("name"); // 'foo' portion of the above URL
    	final String ownerName 		= repository.getJSONObject("owner").getString("login"); // 'kohsuke' portion of the above URL
        
        LOGGER.info("Received POST for "+repoUrl);
        LOGGER.fine("Full details of the POST was "+o.toString());
        Matcher matcher = REPOSITORY_NAME_PATTERN.matcher(repoUrl);
        if (matcher.matches()) {
            // run in high privilege to see all the projects anonymous users don't see.
            // this is safe because when we actually schedule a build, it's a build that can
            // happen at some random time anyway.
            try {
            	LOGGER.info(repoName);
                
                for (AbstractProject<?,?> job : Hudson.getInstance().getAllItems(AbstractProject.class)) {
                	LOGGER.info("find trigger");
                	
                    final GitHubTrigger trigger = (GitHubTrigger) job.getTrigger(triggerClass);
                    if (trigger!=null) {
                        LOGGER.fine("Considering to poke "+job.getFullDisplayName());
                        
                        RepoScm scm = (RepoScm)job.getScm();
                        if (scm.getManifestBaseBranch().equals(base_branch)) {
                         	trigger.onPost(pusherName, o);
                        	LOGGER.info("Poked "+job.getFullDisplayName());
                        }
                        else {
                        	LOGGER.info("not Poked "+job.getFullDisplayName() + " ManifestBaseBranch=" + scm.getManifestBaseBranch() + " but pull_request basebranch="+base_branch);
                        }
                    }else
                    {
                    	LOGGER.info("trigger is null");
                    }
                }
            } finally {
                ;
            }
        } else {
            LOGGER.warning("Malformed repo url "+repoUrl);
            LOGGER.info("Malformed repo url "+repoUrl);
        }
    }
    
    public void processPush(final JSONObject o, Class<? extends Trigger<?>> triggerClass)
    {
    	 
    	final String after 		= o.getString("after");
    	final String before 	= o.getString("before");
    	final String ref 		= o.getString("ref");
    	final String pusherName = o.getJSONObject("pusher").getString("name");
    	
    	LOGGER.info("Received Push from "+ ref);
    	LOGGER.info("Received Push object :"+ o.toString());
    	try {
            for (AbstractProject<?,?> job : Hudson.getInstance().getAllItems(AbstractProject.class)) {
            	LOGGER.info("find trigger");
            	
                final GitHubTrigger trigger = (GitHubTrigger) job.getTrigger(triggerClass);
                if (trigger!=null) {
                    LOGGER.fine("Considering to poke "+job.getFullDisplayName());
                    
                    RepoScm scm = (RepoScm)job.getScm();
                    String monitorRef = "refs/heads/" + scm.getManifestBaseBranch();
                    if (monitorRef.equals(ref)) {
                    	pushStableTimer.cancel();
                    	pushStableTimer.purge();
                    	pushStableTimer = new Timer();
                    	pushStableTimer.schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                            	trigger.onPost(pusherName, o);
                            }
                        }, 600*1000);
                    	LOGGER.info("Poked "+job.getFullDisplayName());
                    }
                    else {
                    	LOGGER.info("not Poked "+job.getFullDisplayName() + " monitorRef=" + monitorRef + " but ref ="+ref);
                    }
                }else
                {
                	LOGGER.info(job.getFullDisplayName() + "does not set this trigger");
                }
            }
        } finally {
            ;
        }
    }
    
    public File getLogFile() {
        return new File("/var/log/github-repo.log");
    }
    
    private static final Logger LOGGER = Logger.getLogger(GithubRepoWebHook.class.getName());
    
    public static GithubRepoWebHook get() {
        return Hudson.getInstance().getExtensionList(RootAction.class).get(GithubRepoWebHook.class);
    }
}
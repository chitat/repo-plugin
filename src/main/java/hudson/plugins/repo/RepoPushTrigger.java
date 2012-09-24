package hudson.plugins.repo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.scm.SCM;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import hudson.triggers.Trigger;
import hudson.model.AbstractProject;
import hudson.util.StreamTaskListener;
import org.kohsuke.stapler.StaplerRequest;

import org.kohsuke.stapler.DataBoundConstructor;

import net.sf.json.JSONObject;

public class RepoPushTrigger extends Trigger<AbstractProject<?,?>> implements GitHubTrigger {
	@DataBoundConstructor
    public RepoPushTrigger() {
    }
    /**
     * Called when a POST is made.
     */
    @Deprecated
    public void onPost() {
        onPost("");
    }
    
    public void onPost(String triggeredByUser){
    	onPost("");
    }
    
	public void onPost(String triggeredByUser, JSONObject json){
        final String pushBy = triggeredByUser;
        final JSONObject push_data= json;
        LOGGER.log(Level.INFO," onPost", json);
        getDescriptor().queue.execute(new Runnable() {
            public void run() {
                try {
                    StreamTaskListener listener = new StreamTaskListener(getLogFile());
                    try {
                        PrintStream logger = listener.getLogger();
                        long start = System.currentTimeMillis();
                        logger.println("Started on "+ DateFormat.getDateTimeInstance().format(new Date()));

 
                        GitHubPushAction action = new GitHubPushAction();
 
                        JSONObject repository 	= push_data.getJSONObject("repository");
                        JSONObject head_commit	= push_data.getJSONObject("head_commit");
                        
                		String sha 				= push_data.getString("after");                        
                		String ref 				= push_data.getString("ref");                        
                        String ownerName 		= repository.getJSONObject("owner").getString("name"); 
                        String pusherName		= head_commit.getJSONObject("committer").getString("username");
                		String repoName		= repository.getString("name");
                		
                        action.addEnv("pusherName", pusherName);
                        action.addEnv("sha", sha);
                        action.addEnv("repoName", repoName);
                        action.addEnv("owner", ownerName);

                        
                        boolean result = job.poll(listener).hasChanges();
                        logger.println("Done. Took "+ Util.getTimeSpanString(System.currentTimeMillis()-start));
                        if(result) {
                            logger.println("Changes found");
                            job.scheduleBuild(job.getQuietPeriod() ,new GithubPushCause(pushBy, ref, repoName), action);
                        } else {
                            logger.println("No changes");
                        }
                    } finally {
                        listener.close();
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE,"Failed to record SCM polling",e);
                    LOGGER.log(Level.INFO," Failed to record SCM polling");
                }
            }
        });
	}
	
    /**
     * Returns the file that records the last/current polling activity.
     */
    public File getLogFile() {
        return new File(job.getRootDir(),"github-push-trigger.log");
    }
    

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    
    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Executors.newSingleThreadExecutor());

        private String hookUrl;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof AbstractProject;
        }

        @Override
        public String getDisplayName() {
            return "Build when a push is issued to GitHub";
        }

        /**
         * Returns the URL that GitHub should post.
         */
        public URL getHookUrl() throws MalformedURLException {
            return hookUrl!=null ? new URL(hookUrl) : new URL(Hudson.getInstance().getRootUrl()+GithubRepoWebHook.get().getUrlName()+'/');
        }

        public boolean hasOverrideURL() {
            return hookUrl!=null;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            JSONObject hookMode = json.getJSONObject("hookMode");
            JSONObject o = hookMode.getJSONObject("hookUrl");
            if (o!=null && !o.isNullObject()) {
                hookUrl = o.getString("url");
            } else {
                hookUrl = null;
            }
            save();
            return true;
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }

        public static boolean allowsHookUrlOverride() {
            return ALLOW_HOOKURL_OVERRIDE;
        }
    }
    public static boolean ALLOW_HOOKURL_OVERRIDE = !Boolean.getBoolean(RepoPushTrigger.class.getName()+".disableOverride");
    private static final Logger LOGGER = Logger.getLogger(RepoPushTrigger.class.getName());
}

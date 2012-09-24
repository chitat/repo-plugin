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

public class RepoPullRequestTrigger extends Trigger<AbstractProject<?,?>> implements GitHubTrigger {
	@DataBoundConstructor
    public RepoPullRequestTrigger() {
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
        final JSONObject pull_request_data= json;
        LOGGER.log(Level.INFO," onPost", json);
        getDescriptor().queue.execute(new Runnable() {
            public void run() {
                try {
                    StreamTaskListener listener = new StreamTaskListener(getLogFile());
                    try {
                        PrintStream logger = listener.getLogger();
                        long start = System.currentTimeMillis();
                        logger.println("Started on "+ DateFormat.getDateTimeInstance().format(new Date()));

                        JSONObject pull_request = pull_request_data.getJSONObject("pull_request");
                        JSONObject repository 	= pull_request_data.getJSONObject("repository");
                        JSONObject head 		= pull_request.getJSONObject("head");
                        JSONObject base			= pull_request.getJSONObject("base");
                        
                        String issueNumber		= Integer.toString(pull_request_data.getInt("number"));

                        String pull_branch 	= head.getString("ref");
                        String pusherName 	= head.getJSONObject("user").getString("login");
                        String avatarUrl 	= head.getJSONObject("user").getString("avatar_url");
                        String sha			= head.getString("sha");
                        String base_branch	= base.getString("ref");
                        
                        String repoUrl 		= repository.getString("html_url"); // something like 'https://github.com/kohsuke/foo'
                        String repoName 	= repository.getString("name"); // 'foo' portion of the above URL
                        String ownerName 	= repository.getJSONObject("owner").getString("login"); // 'kohsuke' portion of the above URL
                        
                        
                        GitHubPullRequestAction action = new GitHubPullRequestAction();
                        
                        action.addEnv("branch", pull_branch);
                        action.addEnv("pusherName", pusherName);
                        action.addEnv("sha", sha);
                        action.addEnv("repoName", repoName);
                        action.addEnv("owner", ownerName);
                        action.addEnv("issueNumber",issueNumber);
                        action.addEnv("avatarUrl",avatarUrl);
                        
                        boolean result = job.poll(listener).hasChanges();
                        logger.println("Done. Took "+ Util.getTimeSpanString(System.currentTimeMillis()-start));
                        if(result) {
                            logger.println("Changes found");
                            job.scheduleBuild(job.getQuietPeriod() ,new GithubPullRequestCause(pushBy, base_branch, pull_branch), action);
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
        return new File(job.getRootDir(),"github-repo.log");
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
            return "Build when a pull request is issued to GitHub";
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
    public static boolean ALLOW_HOOKURL_OVERRIDE = !Boolean.getBoolean(RepoPullRequestTrigger.class.getName()+".disableOverride");
    private static final Logger LOGGER = Logger.getLogger(RepoPullRequestTrigger.class.getName());
}

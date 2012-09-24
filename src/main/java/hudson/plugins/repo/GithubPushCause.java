package hudson.plugins.repo;

import hudson.triggers.SCMTrigger.SCMTriggerCause;

public class GithubPushCause extends SCMTriggerCause {
	
    private String pushedBy;
    private String branch;
    private String repository;

    public GithubPushCause(final String pusher,
    		final String pushto, final String repo) {
        this("", pusher, pushto, repo);
    }
    /**
     * Return a new GithubPushCause.
     * @param pollingLog 	pullinglog
     * @param pusher 		who send the pull request
     * @param pushto		branch which receive the push
     * @param repo 			repository
     */
    public GithubPushCause(final String pollingLog, final String pusher,
    		final String pushto, final String repo) {
        super(pollingLog);
        pushedBy 	= pusher;
        branch		= pushto;
        repository	= repo;
    }
   
    @Override
    public String getShortDescription() {
        String pusher = pushedBy != null ? pushedBy : "";
        return "Started by " + pushedBy + " who push a new commit to branch :" + branch + " of repository: " + repository;
    }
}

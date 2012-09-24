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

import hudson.triggers.SCMTrigger.SCMTriggerCause;

/**
 * Class for record info of pull request.
 * @author chitat@zillians.com
 */
public class GithubPullRequestCause extends SCMTriggerCause {

    private String pushedBy;
    private String baseBranch;
    private String branch;

    /**
     * Return a new GithubPullRequestCause.
     * @param pusher who send the pull request
     * @param base	base branch
     * @param pullBranch which branch is request to be merged
     */
    public GithubPullRequestCause(final String pusher, final String base,
    		final String pullBranch) {
        this("", pusher, base, pullBranch);
    }
    /**
     * Return a new GithubPullRequestCause.
     * @param pollingLog 	pullinglog
     * @param pusher 		who send the pull request
     * @param base			base branch
     * @param pullBranch 	which branch is request to be merged
     */
    public GithubPullRequestCause(final String pollingLog, final String pusher,
    		final String base, final String pullBranch) {
        super(pollingLog);
        pushedBy 	= pusher;
        baseBranch 	= base;
        branch 		= pullBranch;
    }

    @Override
    public String getShortDescription() {
        String pusher = pushedBy != null ? pushedBy : "Someone";
        return pushedBy + " send a pull request want merge " + branch
        		+ " to " + baseBranch;
    }
}

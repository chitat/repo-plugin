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

import hudson.model.Action;
import java.util.HashMap;

/**
 * Action for RepoSCM to get the correct branch and other push information data.
 * @author Danny Lin
 */
public class GitHubPushAction implements Action {
	private HashMap<String, String> map;

	/**
	 * Creates a new GitHubPushAction.
	 */
    public GitHubPushAction() {
    	this.map = new HashMap<String, String>();
    }
	/**
	 * Return a UrlName to use for GitHubPushAction.
	 */
    public String getUrlName() { return "push-action1"; }
	/**
	 * Return DisplayName to use for GitHubPushAction.
	 */
    public String getDisplayName() { return "GitHubPushAction"; }
	/**
	 * Retrun IconFileName to use for GitHubPushAction.
	 */
    public String getIconFileName() { return "notepad.gif"; }
	/**
	 * Return a (key,value) map for retrieve GitHubPushAction information.
	 */
    public HashMap<String, String> getData() {
    	return map;
    }
	/**
	 * Add (key,value) pair to map.
	 * @param key 	this is key.
	 * @param value this is value.
	 */
    public void addEnv(final String key, final String value) {
    	map.put(key, value);
    }
}

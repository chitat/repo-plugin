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

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.List;
/**
 * This is a simple publisher for Github commit status.
 */
public class GithubStatusPublisher
	extends Publisher implements ExtensionPoint {
	private final String login;
	/**
	 * Returns username.
	 */
	public String getLogin() {
		return login;
	}

	private final String password;
	/**
	 * Returns password.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Returns GithubStatusPublisher.
	 * @param login 	username
	 * @param password 	password
	 */
	GithubStatusPublisher(final String login, final String password) {
		this.login           = login;
		this.password        = password;
	}
	private static Logger LOGGER = 
			Logger.getLogger("hudson.plugins.repo.GithubStatusPublisher");

  @Override
  public boolean perform(final AbstractBuild build, final Launcher launcher,
		  final BuildListener listener) {
	  
	  listener.getLogger().println("GithubStatusPublisher: Json object status:"
				+ build.getDurationString());
		
	final GitHubPullRequestAction pull_request_action =
			build.getAction(GitHubPullRequestAction.class);
	
	final GitHubPushAction push_action =
			build.getAction(GitHubPushAction.class);
	
	if (pull_request_action != null) {
		return publishPullRequest(build, launcher, listener);
	} else if (push_action != null) {
		return publishPush(build, launcher, listener);
	} else {
		return false;
	}
	
	
  }
  
  public boolean publishPullRequest(final AbstractBuild build, final Launcher launcher,
		  final BuildListener listener) {
		final GitHubPullRequestAction action =
				build.getAction(GitHubPullRequestAction.class);
	  	
		String status;
		String testResult;
		
		
		if (build.getResult()==Result.SUCCESS) {
			status = "success";
			testResult = "Good Job!";
		} else if (build.getResult()==Result.FAILURE) {
			status = "failure";
			testResult = "Screw Somthing - Build Failed";
		} else if (build.getResult()==Result.UNSTABLE) { 
			status = "error";
			testResult = "Make Build Success - But Test Unstable";
		} else {
			status = "error";
			testResult = "Screw Somthing";
		}
			
		String repoName 	= action.getData().get("repoName");
		String pusherName 	= action.getData().get("pusherName");
		String branch 		= action.getData().get("branch");
		String sha 			= action.getData().get("sha");
		String owner 		= action.getData().get("owner");
		String issueNumber 	= action.getData().get("issueNumber");
		String avatarUrl	= action.getData().get("avatarUrl");
		
	    String buildNumber 	= Integer.toString(build.getNumber());
	    String jobName 		= build.getParent().getDisplayName();
	    
		JSONObject parameter = new JSONObject();
	    parameter.put("state",status);
	    parameter.put("target_url","http://buildbot:8080/job/" + build.getParent().getDisplayName() + "/" + buildNumber + "/console");
	    LOGGER.info("GithubStatusPublisher: Json object: "+parameter.toString());
		String gitHubApi = "repos/"+owner+"/"+repoName+"/statuses/"+sha;
			
		sendDataToGitHub(gitHubApi, parameter);
		

		gitHubApi =	"repos/"+owner+"/"+repoName+"/issues/"+ issueNumber + "/comments";
		JSONObject issue_comments = new JSONObject();
		
		issue_comments.put("body",
				 "![GitHub Logo](" + avatarUrl + ")\n"
				+ "### "+ testResult + "\n" );
		sendDataToGitHub(gitHubApi, issue_comments);
		    
		return true;
  }
  
  public boolean publishPush(final AbstractBuild build, final Launcher launcher,
		  final BuildListener listener) {
		final GitHubPushAction push_action =
				build.getAction(GitHubPushAction.class);
		
		String status;
		String testResult;
		
		if (build.getResult()==Result.SUCCESS) {
			status = "success";
			testResult = "Good Job!";
		} else if (build.getResult()==Result.FAILURE) {
			status = "failure";
			testResult = "Build Failed";
		} else if (build.getResult()==Result.UNSTABLE) { 
			status = "error";
			testResult = "Build Success - But Test Unstable";
		} else {
			status = "error";
			testResult = "Somthing Smashed ";
		}
		
		String sha 			= push_action.getData().get("sha");
		String owner 		= push_action.getData().get("owner");
		String repoName		= push_action.getData().get("repoName");
		
		String buildNumber 	= Integer.toString(build.getNumber());
		
		String gitHubApi = "repos/"+owner+"/"+repoName+"/commits/"+ sha + "/comments";
		JSONObject commit_comments = new JSONObject();
		
		commit_comments.put("body",
				   "### "+ testResult + "\n" );

		sendDataToGitHub(gitHubApi, commit_comments);
		
		return true;
  }
  
  public void sendDataToGitHub(String gitHubApiV3, JSONObject jsonObject) {
	  try{
		String surl = "https://api.github.com/" + gitHubApiV3; 
		String userPassword = login + ":" + password;
		String encoding = new sun.misc.BASE64Encoder().encode(userPassword.getBytes());

	    URL url = new URL(surl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setRequestMethod("POST");
	    conn.setDoOutput(true);
	    conn.setDoInput(true);
	    conn.setUseCaches(false);
	    conn.setRequestProperty("Authorization", "Basic " + encoding);
	    conn.setRequestProperty("Content-Type", "application/json");
	    conn.setRequestProperty("Accept", "application/json");
	    conn.setRequestProperty("Content-Length",
	    		Integer.toString(jsonObject.toString().length()));

	    conn.getOutputStream().write(jsonObject.toString().getBytes());
	    conn.getOutputStream().flush();
	    conn.connect();
	    //LOGGER.info("GithubStatusPublisher jsonObject: " + jsonObject.toString());
	    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    String line;
	    StringBuffer response = new StringBuffer(); 
	    while((line = rd.readLine()) != null) {
	    	response.append(line);
	    	response.append('\r');
	    }
	    rd.close();
	    //LOGGER.info("GithubStatusPublisher response :" + response.toString());
	    String connContentType = conn.getContentType();
	} catch (Exception e)
	{
		 LOGGER.info("GithubStatusPublisher: exception: "+e.toString());;
	}
  }
	/**
	 * Returns getRequiredMonitorService.
	 */
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }
	/**
	 * Returns BuildStepDescriptor.
	 */
  @Override
  public BuildStepDescriptor<Publisher> getDescriptor() {
    return DESCRIPTOR;
  }
	/**
	 * Returns DESCRIPTOR.
	 */
  @Extension
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	/**
	 * Returns DescriptorImpl.
	 */
  public static final class DescriptorImpl
  	extends BuildStepDescriptor<Publisher> {
	/**
	 * Returns DescriptorImpl.
	 */
    DescriptorImpl() {
      super(GithubStatusPublisher.class);
    }
	/**
	 * Returns getDisplayName.
	 */
    public String getDisplayName() {
      return "GithubStatusPublisher";
    }
    @Override
    public String getHelpFile() {
      return "/plugin/githubStatus/help-projectConfig.html";
    }
    @Override
    public GithubStatusPublisher newInstance(final StaplerRequest req,
    		final JSONObject formData) throws FormException {
      return new GithubStatusPublisher(req.getParameter("github.login"),
    		  req.getParameter("github.password"));
      
    }
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

  }
}



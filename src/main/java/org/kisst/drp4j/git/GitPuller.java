package org.kisst.drp4j.git;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.kisst.drp4j.ResourcePuller;

import java.io.File;
import java.io.IOException;

public class GitPuller implements ResourcePuller {
	private final Git git;
	private final File dir;

	public GitPuller(Git git) {
		this.git=git;
		this.dir=git.getRepository().getDirectory();
	}

	public GitPuller(String localPath) {
		try {
			this.git = new Git(new FileRepository(localPath + "/.git"));

		}
		catch (IOException e) { throw new RuntimeException(e); }
		this.dir=git.getRepository().getDirectory();
	}

	public static GitPuller clone(String uri, File localPath) {
		try {
			Git git = Git.cloneRepository()
					.setURI(uri)
					.setDirectory(localPath)
					//.setCredentialsProvider(new UsernamePasswordCredentialsProvider("***", "***"))
					.call();
			return new GitPuller(git);
		} catch (GitAPIException e) { throw new RuntimeException(e);}
	}

	@Override public void pull() {
		try {
			git.pull().call();
		}
		catch (GitAPIException e) { throw new RuntimeException(e); }
	}

	@Override public File getLocalDirectory() {
		return dir;
	}
}

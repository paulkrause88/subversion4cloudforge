package hudson.scm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.model.Build;
import hudson.model.ItemGroup;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.scm.SubversionRepositoryStatus.JobProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @see SubversionRepositoryStatusTest
 */
public class SubversionRepositoryStatusCloudForgeTest {

	@SuppressWarnings("rawtypes")
	static class JobProviderBean implements JobProvider {
		private List<AbstractProject> allJobs = new ArrayList<AbstractProject>();

		public List<AbstractProject> getAllJobs() {
			return Collections.unmodifiableList(allJobs);
		}

		public void addJob(AbstractProject job) {
			allJobs.add(job);
		}
	}
	
	static abstract class MockBuild extends Build<MockProject, MockBuild> {

		protected MockBuild(MockProject project) throws IOException {
			super(project);
		}
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static abstract class MockProject extends Project<MockProject, MockBuild> {

		protected MockProject(ItemGroup parent, String name) {
			super(parent, name);
		}

	}

	static BufferedReader asReader(String string) {
		return new BufferedReader(new StringReader(string));
	}

	private SubversionRepositoryStatusCloudForge repositoryStatus = 
			new SubversionRepositoryStatusCloudForge("cloudforge.com");
	private JobProviderBean jobProvider = new JobProviderBean();

	@Test
	@Bug(15794)
	public void shouldIgnoreDisabledJobs() throws IOException {

		// GIVEN: a disabled project
		final MockProject project = mock(MockProject.class);
		when(project.isDisabled()).thenReturn(true);

		jobProvider.addJob(project);
		repositoryStatus.setJobProvider(jobProvider);

		// WHEN: post-commit hook is triggered
		StaplerRequest request = mock(StaplerRequest.class);
		when(request.getReader()).thenReturn(asReader("/somepath\n"));
		StaplerResponse response = mock(StaplerResponse.class);

		repositoryStatus.doNotifyCommit(request, response);

		// THEN: disabled project is not considered at all
		verify(project, never()).getScm();
	}

}

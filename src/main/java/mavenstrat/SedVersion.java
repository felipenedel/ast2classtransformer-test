package mavenstrat;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.shared.release.strategy.Strategy;

@Named("ascii-art-enhanced-strategy")
@Singleton
public class SedVersion implements Strategy {
	@Named("default")
	@Inject
	Strategy defaultStrategy;

	@Override public List<String> getPreparePhases() {
		System.out.println("---------------");
		System.out.println("--- prepare ---");
		System.out.println("---------------");
		return this.defaultStrategy.getPreparePhases();
	}

	@Override public List<String> getPerformPhases() {
		System.out.println("---------------");
		System.out.println("--- perform ---");
		System.out.println("---------------");
		return this.defaultStrategy.getPerformPhases();
	}

	@Override public List<String> getBranchPhases() {
		System.out.println("---------------");
		System.out.println("--- branch ---");
		System.out.println("---------------");
		return this.defaultStrategy.getBranchPhases();
	}

	@Override public List<String> getRollbackPhases() {
		System.out.println("---------------");
		System.out.println("--- rollback ---");
		System.out.println("---------------");
		return this.defaultStrategy.getRollbackPhases();
	}

	@Override public List<String> getUpdateVersionsPhases() {
		System.out.println("---------------");
		System.out.println("--- update ---");
		System.out.println("---------------");
		return this.defaultStrategy.getUpdateVersionsPhases();
	}
}
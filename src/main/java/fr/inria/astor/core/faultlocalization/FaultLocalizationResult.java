package fr.inria.astor.core.faultlocalization;

import java.util.List;

import fr.inria.astor.core.faultlocalization.entity.SuspiciousTestedCode;

/**
 * Stores the result of a fault localization process
 * @author Matias Martinez
 *
 */
public class FaultLocalizationResult {

	
	List<SuspiciousTestedCode> candidates;
	List<String> failingTestCases;
	
	public FaultLocalizationResult(List<SuspiciousTestedCode> candidates, List<String> failingTestCases) {
		super();
		this.candidates = candidates;
		this.failingTestCases = failingTestCases;
	}

	public List<SuspiciousTestedCode> getCandidates() {
		return candidates;
	}

	public void setCandidates(List<SuspiciousTestedCode> candidates) {
		this.candidates = candidates;
	}

	public List<String> getFailingTestCases() {
		return failingTestCases;
	}

	public void setFailingTestCases(List<String> failingTestCases) {
		this.failingTestCases = failingTestCases;
	}

}

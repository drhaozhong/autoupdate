package fr.inria.astor.core.faultlocalization.entity;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import fr.inria.astor.core.faultlocalization.gzoltar.TestCaseResult;

/**
 * This entity represents a suspicious lines inside a class.
 * 
 * @author Matias Martinez, matias.martinez@inria.fr
 * 
 */
public class SuspiciousTestedCode extends SuspiciousCode{
	/**
	 * Key is the test identifier, value Numbers of time executed by that test.
	 */
	private Map<Integer, Integer> coverage = null;

	protected List<TestCaseResult> coveredByTests = null;
	/**
	 * Suspicious value of the line
	 */
	protected double suspiciousValue;

	public SuspiciousTestedCode() {
		
	}

	public SuspiciousTestedCode(String className, String methodName, int lineNumber, double susp,
			Map<Integer, Integer> frequency) {
		super();
		this.className = className;
		this.methodName = methodName;
		this.lineNumber = lineNumber;
		this.suspiciousValue = susp;
		this.coverage = frequency;
	}

	public SuspiciousTestedCode(String className, String methodName, double susp) {
		super();
		this.className = className;
		this.methodName = methodName;
		this.suspiciousValue = susp;
	}

	public double getSuspiciousValue() {
		return suspiciousValue;
	}

	DecimalFormat df = new DecimalFormat("#.###");

	public String getSuspiciousValueString() {
		return df.format(this.suspiciousValue);
	}

	public void setSusp(double susp) {
		this.suspiciousValue = susp;
	}

	

	@Override
	public String toString() {
		return "Candidate [className=" + className + ", methodName=" + methodName + ", lineNumber=" + lineNumber
				+ ", susp=" + suspiciousValue + "]";
	}

	public Map<Integer, Integer> getCoverage() {
		return coverage;
	}

	public void setCoverage(Map<Integer, Integer> coverage) {
		this.coverage = coverage;
	}

	

	public List<TestCaseResult> getCoveredByTests() {
		return coveredByTests;
	}

	public void setCoveredByTests(List<TestCaseResult> coveredByTests) {
		this.coveredByTests = coveredByTests;
	}

}

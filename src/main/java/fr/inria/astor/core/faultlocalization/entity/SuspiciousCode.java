package fr.inria.astor.core.faultlocalization.entity;

public abstract class SuspiciousCode {
	

	/**
	 * Suspicious class
	 */
	protected String className;

	protected String methodName;
	/**
	 * Suspicious line number
	 */
	protected int lineNumber;


	protected String fileName;
	
	abstract public double getSuspiciousValue();
	abstract public String getSuspiciousValueString();
	
	public int getLineNumber() {
		return lineNumber;
	}

	
	
	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}
	public String getClassName() {
		int i = className.indexOf("$");
		if (i != -1) {
			return className.substring(0, i);
		}

		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}

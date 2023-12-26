package cn.sjtu.autoupdate.apidoc.data;

public class MethodItem extends MethodInfo{
	public double sim;
	
	public MethodItem(String v) {
		super(v);
	}

	public MethodItem(MethodInfo m) {
		super(m.version);
		this.doc = m.doc;
		this.exceptions = m.exceptions;
		this.fullname = m.fullname;
		this.isDeprecated = m.isDeprecated;
		this.name = m.name;
		this.paras = m.paras;
		this.refs = m.refs;
		this.returnvalue = m.returnvalue;
		this.url = m.url;
		this.isStatic = m.isStatic;
	}

	@Override
	public String toString() {
		return sim+":"+fullname;
	}

}

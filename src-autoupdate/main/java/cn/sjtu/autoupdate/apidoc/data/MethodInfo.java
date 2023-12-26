package cn.sjtu.autoupdate.apidoc.data;

import java.io.File;
import java.util.ArrayList;

public class MethodInfo extends AbstractInfo {
	public ArrayList<VarInfo> paras = new ArrayList<VarInfo>();
	public ArrayList<ExceptionInfo> exceptions = new ArrayList<ExceptionInfo>();
	public ArrayList<ReferenceInfo> refs = new ArrayList<ReferenceInfo>();
	public VarInfo returnvalue;
	public boolean isStatic = false;
	
	public MethodInfo(String v) {
		super(v);
		// TODO Auto-generated constructor stub
	}

	
	public void setDeprecated() {	
		this.isDeprecated = true;
		for(VarInfo var: paras){
			var.isDeprecated = true;
		}
	}

	public String getShortName() {	
		String shortname = name;
		int mark = shortname.indexOf("(");
		if(mark>0){
			shortname = shortname.substring(0,mark);
		}
		return shortname;
	}
	
}

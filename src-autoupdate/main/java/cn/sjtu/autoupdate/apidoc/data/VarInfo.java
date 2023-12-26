package cn.sjtu.autoupdate.apidoc.data;

import java.util.ArrayList;

public class VarInfo extends AbstractInfo {
	public String type;
	public String modifier;
	public boolean isCons = false;	
	
	public VarInfo(String v) {
		super(v);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String toString() {
		return type+":"+name;
	}
}

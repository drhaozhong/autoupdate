package cn.sjtu.autoupdate.apidoc.data;

public abstract class AbstractInfo {
	public String url = "";
	public String name = "";
	public boolean isDeprecated = false;
	public String fullname = "";
	public String doc = "";
	public String version;
	
	public AbstractInfo(String v) {
		version = v;
	}
	
	public String getPackageName() {
		return fullname.substring(0, fullname.length()-name.length()-1);
	}
}

package cn.sjtu.autoupdate.apidoc.data;

import cn.sjtu.autoupdate.apidoc.data.AbstractInfo;

public class ApiMapping {
	public static final int CLASS = 1;
	public static final int METHOD = 2;
	public static final int FIELD = 3;
	public static final int PARA = 4;
	
	public int type = 0;
	public AbstractInfo oldItem;
	public AbstractInfo newItem;
	
	public ApiMapping(int t) {
		type = t;
	}

	@Override
	public String toString() {
		String line = "";
		switch(type) {
			case CLASS: line = "class";break;
			case METHOD: line = "method";break;
			case FIELD: line = "field";break;
			case PARA: line = "para";break;
		}
		line += "-------------------------------------\n";
		line += oldItem.fullname+":"+oldItem.doc+"->\n";
		line += newItem.fullname+":"+newItem.doc+"\n";;
		line += "-------------------------------------\n";
		return line;
	}
}

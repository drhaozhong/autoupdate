package cn.sjtu.autoupdate.apidoc.data;

import java.util.ArrayList;
import java.util.Collection;



public class PackageInfo extends AbstractInfo {
	public PackageInfo(String v) {
		super(v);
		// TODO Auto-generated constructor stub
	}

	public ArrayList<TypeInfo> classes = new ArrayList<TypeInfo>(); 
	public ArrayList<TypeInfo> interfaces = new ArrayList<TypeInfo>(); 
	public ArrayList<TypeInfo> exceptions = new ArrayList<TypeInfo>();
	public ArrayList<TypeInfo> enums = new ArrayList<TypeInfo>(); 
	
	public void generateFullTypeNames() {
		// TODO Auto-generated method stub
		for(TypeInfo info:classes){
			if(info.name.indexOf(".")<0){
				info.name = name+"."+info.name;
			}
		}
		for(TypeInfo info:interfaces){
			if(info.name.indexOf(".")<0){
				info.name = name+"."+info.name;
			}
		}
		for(TypeInfo info:exceptions){
			if(info.name.indexOf(".")<0){
				info.name = name+"."+info.name;
			}
		}
	}

	public ArrayList<TypeInfo> getTypes(String name) {
		// TODO Auto-generated method stub
		ArrayList<TypeInfo> types = new ArrayList<TypeInfo>();
		for(TypeInfo t:classes){
			if(t.name.compareTo(name)==0){
				types.add(t);
			}
		}
		for(TypeInfo t:interfaces){
			if(t.name.compareTo(name)==0){
				types.add(t);
			}
		}
		for(TypeInfo t:exceptions){
			if(t.name.compareTo(name)==0){
				types.add(t);
			}
		}
		for(TypeInfo t:enums){
			if(t.name.compareTo(name)==0){
				types.add(t);
			}
		}
		return types;
	}

	public void merge(PackageInfo packageinfo) {
		// TODO Auto-generated method stub
		classes.addAll(packageinfo.classes);
		interfaces.addAll(packageinfo.interfaces);
		exceptions.addAll(packageinfo.exceptions);
		enums.addAll(packageinfo.enums);
	}	
}

package cn.sjtu.autoupdate.apidoc.data;

import java.util.ArrayList;
import java.util.Collection;


public class TypeInfo extends AbstractInfo {
	public TypeInfo(String v) {
		super(v);
		// TODO Auto-generated constructor stub
	}



	public ArrayList<String> superchain = new ArrayList<String>();
	public ArrayList<VarInfo> fields = new ArrayList<VarInfo>();
	public ArrayList<VarInfo> consts = new ArrayList<VarInfo>();
	public ArrayList<MethodInfo> constructors = new ArrayList<MethodInfo>();
	public ArrayList<MethodInfo> methods = new ArrayList<MethodInfo>();
	
	public void setDeprecated() {
		// TODO Auto-generated method stub
		if(doc!=null){
			if(doc.startsWith("Deprecated")){
				this.isDeprecated = true;
				for(VarInfo var:fields){			
					var.isDeprecated = true;
				}
				for(VarInfo var:consts){
					var.isDeprecated = true;
				}
				for(MethodInfo method:constructors){
					method.setDeprecated();
				}
				for(MethodInfo method:methods){
					method.setDeprecated();
				}
			
			}
		}
	}

	public ArrayList<MethodInfo> queryMethod(String methodname) {
		// TODO Auto-generated method stub
		 ArrayList<MethodInfo> ms = new ArrayList<MethodInfo>();
		for(MethodInfo m:this.constructors){
			int mark = m.name.indexOf("(");
			String name = m.name.substring(0, mark);
			if(name.compareTo(methodname)==0){
				ms.add(m);
			}
		}
		for(MethodInfo m:this.methods){
			int mark = m.name.indexOf("(");
			String name = m.name.substring(0, mark);
			if(name.compareTo(methodname)==0){
				ms.add(m);
			}
		}
	
		return ms;
	}

	public ArrayList<VarInfo> queryField(String fieldname) {
		// TODO Auto-generated method stub
		ArrayList<VarInfo> vars = new ArrayList<VarInfo>();
		for(VarInfo f:this.fields){
			if(f.name.compareTo(fieldname)==0){
				vars.add(f);
				break;
			}
		}
		for(VarInfo f:this.consts){
			if(f.name.compareTo(fieldname)==0){
				vars.add(f);
				break;
			}
		}
		return vars;
	}

	

	public void upateFullName() {
		// TODO Auto-generated method stub
		for(VarInfo var:fields){
			var.fullname = this.fullname+"."+var.name;
		}
		for(VarInfo var:consts){
			var.fullname = this.fullname+"."+var.name;
		}
		for(MethodInfo method:constructors){
			method.fullname = this.fullname+"."+method.name;
			for(VarInfo para:method.paras) {
				para.fullname = method.fullname+"."+para.name;
			}
		}
		for(MethodInfo method:methods){
			method.fullname = this.fullname+"."+method.name;
			for(VarInfo para:method.paras) {
				para.fullname = method.fullname+"."+para.name;
			}
		}
		
	}
	
}

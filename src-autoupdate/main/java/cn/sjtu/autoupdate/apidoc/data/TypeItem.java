package cn.sjtu.autoupdate.apidoc.data;

public class TypeItem extends TypeInfo{
	public double sim;
	public TypeItem(TypeInfo t) {
		super(t.version);
		this.constructors = t.constructors;
		this.consts = t.consts;
		this.doc = t.doc;
		this.fields = t.fields;
		this.fullname = t.fullname;
		this.isDeprecated = t.isDeprecated;
		this.methods = t.methods;
		this.name = t.name;
		this.superchain = t.superchain;
		this.url = t.url;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.fullname+":"+sim;
	}
	

}

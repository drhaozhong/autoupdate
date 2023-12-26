package cn.sjtu.autoupdate.repair.space;

import java.util.ArrayList;
import java.util.Hashtable;

import com.db4o.Db4oEmbedded;
import com.db4o.EmbeddedObjectContainer;
import com.db4o.config.EmbeddedConfiguration;

import cn.sjtu.autoupdate.apidoc.data.ApiMapping;
import cn.sjtu.autoupdate.apidoc.data.MethodInfo;
import cn.sjtu.autoupdate.apidoc.data.MethodItem;
import cn.sjtu.autoupdate.apidoc.data.TypeInfo;
import cn.sjtu.autoupdate.apidoc.data.TypeItem;
import cn.sjtu.autoupdate.apidoc.data.VarInfo;
import cn.sjtu.autoupdate.repair.operator.method.BestMethodNameRepairOperator;
import cn.sjtu.autoupdate.repair.operator.method.ConstructorToCreatorRepairOperator;
import cn.sjtu.autoupdate.repair.operator.method.MethodCallRepairOperator;
import cn.sjtu.autoupdate.repair.operator.method.MethodDeleteOperator;
import cn.sjtu.autoupdate.repair.operator.method.MethodMappingRepairOperator;
import cn.sjtu.autoupdate.repair.operator.method.ParameterReducerRepairOperator;
import cn.sjtu.autoupdate.repair.operator.method.SignatureMatchRepairOperator;
import cn.sjtu.autoupdate.repair.operator.method.StaticMethodToNonstaticRepairOperator;
import cn.sjtu.autoupdate.repair.operator.method.UnhandledExceptionRepairOperator;
import cn.sjtu.autoupdate.repair.operator.method.UnimplementRepairOperator;
import cn.sjtu.autoupdate.repair.operator.type.InvalidTypeRemover;
import cn.sjtu.autoupdate.repair.operator.type.TypeConvertRepairOperator;
import cn.sjtu.autoupdate.repair.operator.type.replace.AmbiguousTypeRepairOperator;
import cn.sjtu.autoupdate.repair.operator.type.replace.BestTypeNameRepairOperator;
import cn.sjtu.autoupdate.repair.operator.type.replace.TypeMappingRepairOperator;
import cn.sjtu.autoupdate.repair.operator.variable.FieldMappingRepairOperator;
import cn.sjtu.autoupdate.repair.operator.variable.FieldToMethodRepairOperator;
import cn.sjtu.autoupdate.repair.operator.variable.InvisibleFieldRepairOperator;
import cn.sjtu.autoupdate.repair.operator.variable.TypeVariableFinderOperator;
import fr.inria.astor.approaches.cardumen.ExpressionReplaceOperator;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.solutionsearch.spaces.operators.OperatorSpace;

public class AutoupdateOperatorSpace extends OperatorSpace {
	
	public  AutoupdateOperatorSpace() {
		String file = ConfigurationProperties.getProperty("codemapping");
		EmbeddedConfiguration configuration = Db4oEmbedded.newConfiguration();
		configuration.common().activationDepth(Integer.MAX_VALUE);
		EmbeddedObjectContainer container = Db4oEmbedded.openFile(configuration, file);
		Hashtable<String, String> codemappings = new Hashtable<String, String>();
		Hashtable<String, ArrayList<String>> shortmappings = new Hashtable<String, ArrayList<String>>(); 
		
		for(Object o:container.query(ApiMapping.class)) {
			ApiMapping mapping = (ApiMapping)o;
			if(mapping.oldItem.fullname.compareTo(mapping.newItem.fullname)!=0&&!mapping.newItem.fullname.isEmpty()) {
				String key = mapping.oldItem.fullname;
				String value = mapping.newItem.fullname;
				if(mapping.type==ApiMapping.METHOD) {
					int mark = key.lastIndexOf("(");
					key = key.substring(0, mark);
					mark = value.lastIndexOf("(");
					value = value.substring(0, mark);
				}				
				codemappings.put(key, value);
				int mark = key.lastIndexOf(".");
				String shortkey = key.substring(mark+1);
				ArrayList<String> list = shortmappings.get(shortkey);
				if(list==null) {
					list = new ArrayList<String>();
				}
				list.add(value);
				shortmappings.put(shortkey, list);				
			}
		}
		container.close();
		
		file = ConfigurationProperties.getProperty("olddoc");
		configuration = Db4oEmbedded.newConfiguration();
		configuration.common().activationDepth(Integer.MAX_VALUE);		
		container = Db4oEmbedded.openFile(configuration, file);
		Hashtable<String, String> oldFieldTypes = new Hashtable<String, String>();		
		
		for(Object o:container.query(VarInfo.class)) {
			VarInfo fi = (VarInfo)o; 
			if(fi.type!=null&&!fi.fullname.isEmpty()) {
				oldFieldTypes.put(fi.fullname, fi.type);
			}
		}
		
	
		
		Hashtable<String, ArrayList<MethodInfo>> oldMethods = new Hashtable<String, ArrayList<MethodInfo>>(); 
		for(Object o:container.query(MethodInfo.class)) {
			MethodInfo method = (MethodInfo)o;
			int mark = method.name.indexOf("(");
			String key = method.name.substring(0, mark);
			ArrayList<MethodInfo> list = oldMethods.get(key);
			if(list==null) {
				list = new ArrayList<MethodInfo>();
			}
			
			list.add(method);
			
			oldMethods.put(key, list);
		}
		
		container.close();
		
		file = ConfigurationProperties.getProperty("newdoc");
		configuration = Db4oEmbedded.newConfiguration();
		configuration.common().activationDepth(Integer.MAX_VALUE);		
		container = Db4oEmbedded.openFile(configuration, file);
		ArrayList<MethodItem> newMethods = new ArrayList<MethodItem>();		
	
		for(Object o:container.query(MethodInfo.class)) {
			MethodInfo m = (MethodInfo)o;
			newMethods.add(new MethodItem(m));
		}
		
		ArrayList<TypeItem> newClasses = new ArrayList<TypeItem>();
		for(Object o:container.query(TypeInfo.class)) {
			TypeInfo t = (TypeInfo)o;
			if(!t.fullname.isEmpty()) {
				newClasses.add(new TypeItem(t));
			}
		}
		
		container.close();
		
		file = ConfigurationProperties.getProperty("j2se");
		configuration = Db4oEmbedded.newConfiguration();
		configuration.common().activationDepth(Integer.MAX_VALUE);		
		container = Db4oEmbedded.openFile(configuration, file);
		
		for(Object o:container.query(MethodInfo.class)) {
			MethodInfo m = (MethodInfo)o;
			newMethods.add(new MethodItem(m));
		}
		for(Object o:container.query(TypeInfo.class)) {
			TypeInfo t = (TypeInfo)o;
			if(!t.fullname.isEmpty()) {
				newClasses.add(new TypeItem(t));
			}
		}
		
		container.close();
		
		super.register(new ParameterReducerRepairOperator());
		super.register(new MethodCallRepairOperator());
		super.register(new FieldToMethodRepairOperator(oldFieldTypes));
		
		super.register(new AmbiguousTypeRepairOperator(newClasses));
		super.register(new TypeMappingRepairOperator(shortmappings));
		super.register(new BestTypeNameRepairOperator(newClasses, 2));
		super.register(new MethodMappingRepairOperator(codemappings,shortmappings));
		super.register(new BestMethodNameRepairOperator(newMethods, 2));
		super.register(new FieldMappingRepairOperator(codemappings,shortmappings));
		
		super.register(new InvisibleFieldRepairOperator(oldFieldTypes));
		super.register(new UnimplementRepairOperator());
		super.register(new TypeConvertRepairOperator());
		super.register(new StaticMethodToNonstaticRepairOperator());
		super.register(new SignatureMatchRepairOperator(newMethods, 10));
		super.register(new ConstructorToCreatorRepairOperator());
		super.register(new UnhandledExceptionRepairOperator(newClasses));
		super.register(new InvalidTypeRemover());
		super.register(new TypeVariableFinderOperator(newClasses));
		super.register(new MethodDeleteOperator());
	}

}

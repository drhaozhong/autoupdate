package cn.sjtu.autoupdate.repair.operator;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import fr.inria.astor.core.solutionsearch.spaces.operators.AutonomousOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtConstructorCallImpl;
import spoon.support.reflect.code.CtFieldReadImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.code.CtLiteralImpl;
import spoon.support.reflect.code.CtLocalVariableImpl;
import spoon.support.reflect.code.CtTypeAccessImpl;
import spoon.support.reflect.code.CtVariableReadImpl;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public abstract class TypeMatchOperator extends AutonomousOperator {
	protected Hashtable<String, String> typeTable = new Hashtable<String, String>();
	protected Levenshtein stringComparator = new Levenshtein();
	
	public TypeMatchOperator() {
		typeTable.put("Integer", "int");
		typeTable.put("Double", "double");
		typeTable.put("Float", "float");
		typeTable.put("Byte", "byte");
		typeTable.put("Boolean", "boolean");
		typeTable.put("Short", "short");
		typeTable.put("Long", "long");
		typeTable.put("Character", "char");
		
	}
	
	protected String resolveExpectedReturnType(CtInvocationImpl invoc) {
		CtElement parent = invoc.getParent();
		if(parent instanceof CtInvocationImpl) {
			return resolveMethodInvoc(invoc, (CtInvocationImpl)parent);
		}else if(parent instanceof CtConstructorCallImpl) {
			return resolveConstructorInvoc(invoc, (CtConstructorCallImpl)parent);
		}else if(parent instanceof CtLocalVariableImpl) {
			return resolveLocalVariable((CtLocalVariableImpl)parent);
		}
		return null;
	}

	private String resolveLocalVariable(CtLocalVariableImpl variable) {
		CtTypeReference type = variable.getType();
		return type.getSimpleName();
	}

	private String resolveConstructorInvoc(CtInvocationImpl invoc, CtConstructorCallImpl parent) {
		int index = -1;
		for(int i=0; i<parent.getArguments().size(); i++) {
			Object arg = parent.getArguments().get(i);
			if(arg.toString().compareTo(invoc.toString())==0) {
				index = i;
				break;
			}
		}
		CtExecutableReference pe = parent.getExecutable();
		Object para = pe.getParameters().get(index);
		return para.toString();
	}

	private String resolveMethodInvoc(CtInvocationImpl invoc, CtInvocationImpl parent) {
		int index = -1;
		for(int i=0; i<parent.getArguments().size(); i++) {
			Object arg = parent.getArguments().get(i);
			if(arg.toString().compareTo(invoc.toString())==0) {
				index = i;
				break;
			}
		}
		CtExecutableReference pe = parent.getExecutable();
		if(index<0) {
			return null;
		}
		if(index<pe.getParameters().size()&&pe.getParameters().size()==parent.getArguments().size()) {
			Object para = pe.getParameters().get(index);
			return para.toString();
		}else {
			return null;
		}
		
	}
	
	protected boolean iscompatible(String newType, String oldType) {
		
		if(newType.compareTo(oldType)==0) {
			return true;
		}
		
		int mark = newType.lastIndexOf(".");
		if(mark>0) {
			newType = newType.substring(mark+1);
		}
		mark = oldType.lastIndexOf(".");
		if(mark>0) {
			oldType = oldType.substring(mark+1);
		}
		oldType = oldType.replaceAll("<.>", "");
		oldType = oldType.replaceAll("<.+>", "");
		if(newType.compareTo(oldType)==0) {
			return true;
		}
		String sName = typeTable.get(oldType);
		if(sName!=null&&newType.compareTo(sName)==0) {
			return true;
		}
		return false;
	}
	
	protected String getReturnType(Object o) {
		String type = null;
		if(o instanceof CtFieldReadImpl) {
			CtFieldReadImpl fr = (CtFieldReadImpl)o;
			CtTypeReference t = fr.getVariable().getType();
			type = t.getSimpleName();
		}else if(o instanceof CtInvocationImpl) {
			CtInvocationImpl invoc = (CtInvocationImpl)o;
			type = invoc.getType().getSimpleName();
		}else if(o instanceof CtLiteralImpl) {
			CtLiteralImpl literal = (CtLiteralImpl)o;
			type = literal.getType().getSimpleName();
		}else if(o instanceof CtVariableReadImpl) {
			CtVariableReadImpl fr = (CtVariableReadImpl)o;
			CtTypeReference t = fr.getVariable().getType();
			type = t.getSimpleName();
		}else if(o instanceof CtConstructorCallImpl) {
			CtConstructorCallImpl con = (CtConstructorCallImpl)o;
			CtTypeReference t = con.getType();
			type = t.getSimpleName();
		}
		return type;
	}
	
	protected List generateFeasibleArguments(CtExecutableReference method, List arguments) {
		List args = new ArrayList();
		for(Object o1:method.getParameters()) {
			CtTypeReferenceImpl type1 = (CtTypeReferenceImpl)o1;
			Object arg = null;
			while(arg==null) {
				for(Object o2:arguments) {
					String type2 = getReturnType(o2);
					if(type1.getSimpleName().compareTo(type2)==0) {
						Random rand = new Random();
						int value = rand.nextInt(10);
						if(value<5) {
							arg = o2;
							break;
						}
					}
				}
			}
			args.add(arg);
		}
		return args;
	}
	
	protected boolean isSubset(ArrayList<String> subset, ArrayList<String> superset) {
		if(subset.size()>superset.size()) {
			return false;
		}
		boolean isSubset = true;
		for(String value:subset) {
			if(!superset.contains(value)) {
				isSubset = false;
				for(String supername:superset) {
					if(supername.indexOf(value)>=0||value.indexOf(supername)>=0) {
						isSubset = true;
					}
				}
				break;
			}
		}
		return isSubset;
	}
	
	protected int countSubset(ArrayList<String> types1, ArrayList<String> types2) {
		int common = 0;
		for(String type1:types1) {
			if(types2.contains(type1)) {
				common++;
			}
		}
		return common;
	}
	
	protected CtInvocationImpl getInvocation(CtElement element) {
		while(element!=null&&!(element instanceof CtInvocationImpl)) {
			element = element.getParent();
		}
		return (CtInvocationImpl)element;
	}
	
	protected CtElement getInvocationOrConstructor(CtElement element) {
		while(element!=null&&!(element instanceof CtInvocationImpl)&&!(element instanceof CtConstructorCallImpl)) {
			element = element.getParent();
		}
		return element;
	}
	
	protected CtTypeReferenceImpl getExpectArgumentType(CtElement element) {
		CtElement ioc = getInvocationOrConstructor(element);
		CtTypeReferenceImpl type = null;
		if(ioc instanceof CtInvocationImpl) {
			CtInvocationImpl inv = (CtInvocationImpl)ioc;
			List args = inv.getArguments();
			List paras = inv.getExecutable().getParameters();
			type = resolveArgumentType(element, args, paras);			
		}else if(ioc instanceof CtConstructorCallImpl) {
			CtConstructorCallImpl inv = (CtConstructorCallImpl)ioc;
			List args = inv.getArguments();
			List paras = inv.getExecutable().getParameters();
			type = resolveArgumentType(element, args, paras);
		}
		return type;
	}
	
	private CtTypeReferenceImpl resolveArgumentType(CtElement element, List args, List paras) {
		CtTypeReferenceImpl type = null;
		for(int i=0; i<args.size(); i++) {
			if(element.toString().compareTo(args.get(i).toString())==0) {
				if(i<paras.size()) {
					Object para = paras.get(i);
					if(para instanceof CtTypeReferenceImpl) {
						type = (CtTypeReferenceImpl) para;
						break;
					}
				}
			}
		}
		return type;
	}

	protected CtConstructorCallImpl getConstructor(CtElement element) {
		while(element!=null&&!(element instanceof CtConstructorCallImpl)) {
			element = element.getParent();
		}
		return (CtConstructorCallImpl) element;
	}

	protected CtElement getConstructor(CtElement element, String typeName) {
		while(element!=null&&element.toString().indexOf(typeName)<0) {
			element = element.getParent();
		}
		while(element!=null&&!(element instanceof CtConstructorCallImpl)) {
			element = element.getParent();
		}
		
		return (CtConstructorCallImpl) element;
	}
	
	
	protected CtMethodImpl getMethodBody(CtElement element) {
		while(element!=null&&!(element instanceof CtMethodImpl)) {
			element = element.getParent();
		}
		return (CtMethodImpl)element;
	}

	protected CtClass getClass(CtElement element) {
		while(element!=null&&!(element instanceof CtClass)) {
			element = element.getParent();
		}
		return (CtClass)element;
	}
}

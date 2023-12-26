package cn.sjtu.autoupdate.repair.operator.type.replace;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

import cn.sjtu.autoupdate.repair.operator.type.TypeRepairOperator;

public class TypeReplacerRepairOperator extends TypeRepairOperator {
	private Hashtable<String, String> typeTable = new Hashtable<String, String>();
	private Hashtable<String, Integer> generationTable = new Hashtable<String, Integer>();
	
	protected String selectNewType(ArrayList<String> newTypes, String oldName, int currentGeneration) {
		String newType = null;
		if(newTypes!=null&&newTypes.size()>0) {
			Integer generation = generationTable.get(oldName);
			if(generation==null||generation != currentGeneration) {
				generation = currentGeneration;
				generationTable.put(oldName, generation);
				Random rand = new Random();
				newType = newTypes.get(rand.nextInt(newTypes.size()));
				typeTable.put(oldName, newType);
			}
			newType = typeTable.get(oldName);
			if(newType==null) {
				Random rand = new Random();
				newType = newTypes.get(rand.nextInt(newTypes.size()));
				typeTable.put(oldName, newType);
			}
		}	
		return newType;
	}
}

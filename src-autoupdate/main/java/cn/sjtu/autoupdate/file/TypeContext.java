package cn.sjtu.autoupdate.file;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;

public class TypeContext {
	CtType<?> type;
	CtTypeReference<?> typeRef;
	Set<String> memberNames;

	TypeContext(CtType<?> p_type) {
		type = p_type;
		typeRef = type.getReference();
	}

	public boolean isNameConflict(String name) {
		if (memberNames == null) {
			Collection<CtFieldReference<?>> allFields = type.getAllFields();
			memberNames = new HashSet<>(allFields.size());
			for (CtFieldReference<?> field : allFields) {
				memberNames.add(field.getSimpleName());
			}
		}
		return memberNames.contains(name);
	}

	public String getSimpleName() {
		return typeRef.getSimpleName();
	}

	public CtPackageReference getPackage() {
		return typeRef.getPackage();
	}
}

package cn.sjtu.autoupdate.compile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.filter.NamedElementFilter;

public class JdtImportBuilder {
	private final CompilationUnitDeclaration declarationUnit;
	private String filePath;
	private CompilationUnit spoonUnit;
	private ICompilationUnit sourceUnit;
	private Factory factory;
	private Set<CtImport> imports;

	JdtImportBuilder(CompilationUnitDeclaration declarationUnit,  Factory factory) {
		this.declarationUnit = declarationUnit;
		this.factory = factory;
		this.sourceUnit = declarationUnit.compilationResult.compilationUnit;
		this.filePath = CharOperation.charToString(sourceUnit.getFileName());
		// get the CU: it has already been built during model building in JDTBasedSpoonCompiler
		this.spoonUnit = factory.CompilationUnit().getOrCreate(filePath);
		this.imports = new HashSet<>();
	}

	// package visible method in a package visible class, not in the public API
	void build() {
		// sets the imports of the Spoon compilation unit corresponding to `declarationUnit`

		if (declarationUnit.imports == null || declarationUnit.imports.length == 0) {
			return;
		}

		for (ImportReference importRef : declarationUnit.imports) {
			String importName = importRef.toString();
			if (!importRef.isStatic()) {
				if (importName.endsWith("*")) {
					int lastDot = importName.lastIndexOf(".");
					String packageName = importName.substring(0, lastDot);

					// only get package from the model by traversing from rootPackage the model
					// it does not use reflection to achieve that
					CtPackage ctPackage = this.factory.Package().get(packageName);
					if(ctPackage==null) {
						ctPackage = this.factory.Core().createPackage();
						ctPackage.setSimpleName(packageName);
					}
					this.imports.add(factory.Type().createImport(ctPackage.getReference()));

				} else {
					CtType klass = this.getOrLoadClass(importName);
					if(klass==null) {
						int mark = importName.lastIndexOf(".");
						String packName = importName.substring(0, mark);
						String typeName = importName.substring(mark+1);
						CtPackage ctPackage = this.factory.Package().get(packName);
						if(ctPackage==null) {
							ctPackage = this.factory.Core().createPackage();
							ctPackage.setSimpleName(packName);
						}
						klass = this.factory.Core().createClass();
						klass.setParent(ctPackage);
						klass.setSimpleName(typeName);
					}

					this.imports.add(factory.Type().createImport(klass.getReference()));

				}
			} else {
				int lastDot = importName.lastIndexOf(".");
				String className = importName.substring(0, lastDot);
				String methodOrFieldName = importName.substring(lastDot + 1);

				CtType klass = this.getOrLoadClass(className);
				if (klass != null) {
					if (methodOrFieldName.equals("*")) {
						this.imports.add(factory.Type().createImport(factory.Type().createWildcardStaticTypeMemberReference(klass.getReference())));
					} else {
						List<CtNamedElement> methodOrFields = klass.getElements(new NamedElementFilter<>(CtNamedElement.class, methodOrFieldName));

						if (methodOrFields.size() > 0) {
							CtNamedElement methodOrField = methodOrFields.get(0);
							this.imports.add(factory.Type().createImport(methodOrField.getReference()));
						}
					}
				}
			}
		}
		
		spoonUnit.setImports(this.imports);
	}

	private CtType getOrLoadClass(String className) {
		CtType klass = this.factory.Type().get(className);

		if (klass == null) {
			klass = this.factory.Interface().get(className);

			if (klass == null) {
				try {
					Class zeClass = this.getClass().getClassLoader().loadClass(className);
					klass = this.factory.Type().get(zeClass);
					return klass;
				} catch (NoClassDefFoundError | ClassNotFoundException e) {
					// in some cases we want to import an inner class.
					if (!className.contains(CtType.INNERTTYPE_SEPARATOR) && className.contains(CtPackage.PACKAGE_SEPARATOR)) {
						int lastIndexOfDot = className.lastIndexOf(CtPackage.PACKAGE_SEPARATOR);
						String classNameWithInnerSep = className.substring(0, lastIndexOfDot) + CtType.INNERTTYPE_SEPARATOR + className.substring(lastIndexOfDot + 1);
						return getOrLoadClass(classNameWithInnerSep);
					}
					return null;
				}
			}
		}
		return klass;
	}
}

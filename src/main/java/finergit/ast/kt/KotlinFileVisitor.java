package finergit.ast.kt;

import finergit.FinerGitConfig;
import finergit.ast.FinerModule;
import finergit.ast.j.FinerJavaClass;
import finergit.ast.j.FinerJavaField;
import finergit.ast.j.FinerJavaFile;
import finergit.ast.j.FinerJavaMethod;
import finergit.ast.kt.token.CodeBlock;
import finergit.util.EnvironmentManager;
import finergit.util.KotlinLightVirtualFile;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtilRt;
import org.jetbrains.kotlin.com.intellij.openapi.vfs.CharsetToolkit;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory;
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiFileFactoryImpl;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtAnnotation;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassBody;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtImportList;
import org.jetbrains.kotlin.psi.KtModifierList;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry;
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid;
import org.jetbrains.kotlin.psi.KtTypeElement;
import org.jetbrains.kotlin.psi.KtTypeParameter;
import org.jetbrains.kotlin.psi.KtTypeReference;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import static org.jetbrains.kotlin.lexer.KtTokens.*;


/**
 * Parses and processes the files written in Kotlin.
 */
public class KotlinFileVisitor {

    private FinerGitConfig config;

    private final Stack<FinerModule> moduleStack;
    private final List<FinerModule> moduleList;
    private final Stack<Class<?>> contexts;
    private int classNestLevel;


    public KotlinFileVisitor(final Path path, FinerGitConfig config) {

        this.config = config;
        this.moduleStack = new Stack<>();
        this.moduleList = new ArrayList<>();
        this.contexts = new Stack<>();
        this.classNestLevel = 0;

        final Path dirName = path.getParent();
        final String fileName = FilenameUtils.getBaseName(path.toString());
        final FinerKotlinFile finerKotlinFile = new FinerKotlinFile(dirName, fileName, config);
        this.moduleStack.push(finerKotlinFile);
        this.moduleList.add(finerKotlinFile);
    }


    public List<FinerModule> visit(String filePath, String text) throws IOException {
        KtFile ktFile = (KtFile) buildPsiFile(filePath, EnvironmentManager.createKotlinCoreEnvironment(new HashSet<>()),
                text);

        List<String> importedTypes = processImports(ktFile);
        PsiElement[] elementsInFile = ktFile.getChildren();
        List<KtNamedFunction> packageLevelFunctions = new ArrayList<>();

        for (PsiElement psiElement : elementsInFile) {
            if (psiElement instanceof KtObjectDeclaration) {
                KtObjectDeclaration objectDeclaration = (KtObjectDeclaration) psiElement;
                processObject(objectDeclaration,
                        filePath);
            } else if (psiElement instanceof KtClass) {
                KtClass ktClass = (KtClass) psiElement;
                if (ktClass.isEnum()) {
                    processKtEnum(ktClass, ktFile.getPackageFqName().asString(), filePath,
                        importedTypes);
                } else {
                    processKtClass(ktClass, ktFile.getPackageFqName().asString(), filePath,
                        importedTypes);
                }
            } else if (psiElement instanceof KtNamedFunction) {
                packageLevelFunctions.add((KtNamedFunction) psiElement);
            }
        }
        if (packageLevelFunctions.size() > 0) {
            processPackageLevelFunctions(ktFile, packageLevelFunctions, filePath);
        }

        return this.moduleList.stream()
                .filter(m -> (FinerKotlinFile.class == m.getClass() && config.isPeripheralFileGenerated())
                        || (FinerKotlinClass.class == m.getClass() && config.isClassFileGenerated())
                        || (FinerKotlinMethod.class == m.getClass() && config.isMethodFileGenerated())
                        || (FinerKotlinField.class == m.getClass() && config.isFieldFileGenerated()))
                .collect(Collectors.toList());
    }


    private void processPackageLevelFunctions(KtFile ktFile, List<KtNamedFunction> packageLevelFunctions,
                                              String filePath) {
        LocationInfo locationInfo = generateLocationInfo(ktFile, ktFile.getVirtualFilePath(), ktFile,
            CodeElementType.TYPE_DECLARATION);
        //umlFile.setLocationInfo(locationInfo);
        for (KtNamedFunction function : packageLevelFunctions) {
//            UMLOperation umlOperation = processMethodDeclaration(ktFile, function,
//                false,
//                filePath);
//            umlOperation.setClassName(ktFile.getName());
//            umlFile.addMethod(umlOperation);
        }
        //this.getUmlModel().addFile(umlFile);
    }


    public List<String> processImports(KtFile ktFile) {
        List<String> importedTypes = new ArrayList<>();
        KtImportList importList = ktFile.getImportList();
        if (importList != null) {
            for (KtImportDirective importDeclaration : importList.getImports()) {
                FqName fqName = importDeclaration.getImportedFqName();
                String importName = fqName == null ? null : fqName.asString();
                if (importName != null) {
                    importedTypes.add(importName);
                }
            }
        }
        return importedTypes;
    }


    public void processKtEnum(KtClass ktEnum, String packageName, String sourceFile, List<String> importedTypes) {
        //UMLJavadoc javadoc = generateDocComment(ktEnum);
        String className = ktEnum.getName();
        LocationInfo locationInfo = generateLocationInfo(ktEnum.getContainingKtFile(), sourceFile, ktEnum,
            CodeElementType.TYPE_DECLARATION);
//        UMLClass umlClass = new UMLClass(packageName, className, locationInfo, ktEnum.isTopLevel(), importedTypes);
//        umlClass.setJavadoc(javadoc);
//
//        umlClass.setEnum(true);
//        umlClass.setVisibility(extractVisibilityModifier(ktEnum));
//        //TODO: process body declarations
//
//        this.getUmlModel().addClass(umlClass);
    }


    public void processKtClass(KtClass ktClass, String packageName, String sourceFile, List<String> importedTypes) {
        String className = ktClass.getName();
        LocationInfo locationInfo = generateLocationInfo(ktClass.getContainingKtFile(), sourceFile, ktClass,
            CodeElementType.TYPE_DECLARATION);


        if (ktClass.isData()) {
        } else if (ktClass.isSealed()) {
        } else if (ktClass.isInner()) {
        }

        FinerKotlinClass kclass = new FinerKotlinClass(className, this.moduleStack.peek(), this.config);
        kclass.addToken(new CodeBlock(ktClass.getText()));
        this.moduleList.add(kclass);

        KtModifierList ktClassExtendedModifiers = ktClass.getModifierList();
        if (ktClassExtendedModifiers != null) {
            for (KtAnnotation annotation : ktClassExtendedModifiers.getAnnotations()) {
                //umlClass.addAnnotation(new UMLAnnotation(ktClass.getContainingKtFile(), sourceFile, annotation));
            }
        }

        List<KtTypeParameter> parameters = ktClass.getTypeParameters();

        for (KtTypeParameter parameter : parameters) {
            KtModifierList parameterModifierList = parameter.getModifierList();
            if (parameterModifierList != null) {
                for (KtAnnotation annotation : parameterModifierList.getAnnotations()) {
//                    umlTypeParameter.addAnnotation(
//                        new UMLAnnotation(ktClass.getContainingKtFile(), sourceFile, annotation));

                }
            }
        }

        List<KtProperty> ktClassProperties = ktClass.getProperties();
        for (KtProperty ktProperty : ktClassProperties) {
//            UMLAttribute attribute =
//                processFieldDeclaration(ktClass.getContainingKtFile(), ktProperty, sourceFile);
//            attribute.setClassName(umlClass.getQualifiedName());
//            umlClass.addAttribute(attribute);
        }

        List<KtDeclaration> declarations = ktClass.getDeclarations();
        for (KtDeclaration declaration : declarations) {
            if (declaration instanceof KtNamedFunction) {
                KtNamedFunction function = (KtNamedFunction) declaration;
                    processMethodDeclaration(ktClass.getContainingKtFile(), function, false, sourceFile);
            }
        }

        List<KtObjectDeclaration> companionObjects = ktClass.getCompanionObjects();
        for (KtObjectDeclaration companionObject : companionObjects) {
//            UMLCompanionObject umlCompanionObject = processCompanionObject(companionObject, sourceFile);
//            umlCompanionObject.setClassName(umlClass.getQualifiedName());
//            umlClass.addCompanionObject(umlCompanionObject);
        }

//        this.getUmlModel().addClass(umlClass);
    }

    private Object processFieldDeclaration(KtFile ktFile,
                                                 KtProperty fieldDeclaration,
                                                 String sourceFile) {
//        UMLJavadoc javadoc = generateDocComment(fieldDeclaration);
        //TODO: figure out how to get dimensions
//        UMLType type = UMLType.extractTypeObject(ktFile, sourceFile, fieldDeclaration.getTypeReference(), 0);
        String fieldName = fieldDeclaration.getName();
        LocationInfo locationInfo =
            generateLocationInfo(ktFile, sourceFile, fieldDeclaration, CodeElementType.FIELD_DECLARATION);
//        UMLAttribute umlAttribute = new UMLAttribute(fieldName, type, locationInfo);
//        VariableDeclaration variableDeclaration = new VariableDeclaration(ktFile, sourceFile, fieldDeclaration);
//        variableDeclaration.setAttribute(true);
//        umlAttribute.setVariableDeclaration(variableDeclaration);
//        umlAttribute.setJavadoc(javadoc);
//        umlAttribute.setVisibility(extractVisibilityModifier(fieldDeclaration));
        return null;
    }

    private Object generateDocComment(KtNamedDeclaration bodyDeclaration) {
        Object doc = null;
        KDoc javaDoc = bodyDeclaration.getDocComment();
        if (javaDoc != null) {
//            doc = new UMLJavadoc();
            KDocSection tag = javaDoc.getDefaultSection();
//            UMLTagElement tagElement = new UMLTagElement(tag.getName());
            String fragments = tag.getContent();
//            tagElement.addFragment(fragments);
//            doc.addTag(tagElement);
        }
        return doc;
    }

    private void processMethodDeclaration(KtFile ktFile,
                                                  KtNamedFunction methodDeclaration,
                                                  boolean isInterfaceMethod,
                                                  String filePath) {
        String methodName = methodDeclaration.getName();
        LocationInfo locationInfo = generateLocationInfo(ktFile.getContainingKtFile(), filePath, methodDeclaration,
            CodeElementType.METHOD_DECLARATION);

        final StringBuilder methodFileName = new StringBuilder();

        if (this.config.isAccessModifierIncluded()) {
            KtModifierList methodModifiers = methodDeclaration.getModifierList();
            if (isInterfaceMethod) {
                methodFileName.append("public_");
            } else {
                if (methodModifiers != null) {
                    if (methodModifiers.hasModifier(PUBLIC_KEYWORD)) {
                        methodFileName.append("public_");
                    } else if (methodModifiers.hasModifier(PROTECTED_KEYWORD)) {
                        methodFileName.append("protected_");
                    } else if (methodModifiers.hasModifier(PRIVATE_KEYWORD)) {
                        methodFileName.append("private_");
                    }
                }
            }
        }

        if (this.config.isReturnTypeIncluded()) { // 返り値の型を名前に入れる場合
            if (methodDeclaration.hasDeclaredReturnType()) {
                KtTypeReference returnTypeReference = methodDeclaration.getTypeReference();
                if (returnTypeReference != null) {
                    KtTypeElement typeElement = returnTypeReference.getTypeElement();
                    if (typeElement != null) {

                        final String type = typeElement.getText()
                                .replace(' ', '-') // avoiding space existences
                                .replace('?', '#') // for window's file system
                                .replace('<', '[') // for window's file system
                                .replace('>', ']'); // for window's file system
                        methodFileName.append(type);
                        methodFileName.append("_");
                    }
                }
            }
        }

        methodFileName.append(methodName);
        methodFileName.append("(");

        final List<String> types = new ArrayList<>();

        List<KtParameter> parameters = methodDeclaration.getValueParameters();
        for (KtParameter parameter : parameters) {
            KtTypeReference typeReference = parameter.getTypeReference();
            //String paramName = parameter.getName();

            KtTypeElement typeElement = typeReference.getTypeElement();
            if (typeElement != null) {

                final StringBuilder typeText = new StringBuilder();

                typeText.append(typeElement.getText());
                final String type = typeText.toString()
                        .replace(' ', '-') // avoiding space existences
                        .replace('?', '#') // for window's file system
                        .replace('<', '[') // for window's file system
                        .replace('>', ']'); // for window's file system
                types.add(type);
            }

        }
        methodFileName.append(String.join(",", types));
        methodFileName.append(")");

        FinerKotlinMethod kmethod = new FinerKotlinMethod(methodFileName.toString(), this.moduleStack.peek(), this.config);
        kmethod.addToken(new CodeBlock(methodDeclaration.getText()));
        this.moduleList.add(kmethod);

    }

    public Object processCompanionObject(KtObjectDeclaration object, String sourceFile) {
//        UMLCompanionObject umlCompanionObject = new UMLCompanionObject();
//        umlCompanionObject.setName(object.getName());
        LocationInfo objectLocationInfo = generateLocationInfo(object.getContainingKtFile(), sourceFile, object,
            CodeElementType.COMPANION_OBJECT);
//        umlCompanionObject.setLocationInfo(objectLocationInfo);
        List<KtDeclaration> declarations = object.getDeclarations();
        for (KtDeclaration declaration : declarations) {
            LocationInfo locationInfo = generateLocationInfo(declaration.getContainingKtFile(), sourceFile, declaration,
                CodeElementType.METHOD_DECLARATION);
  //          UMLOperation method = new UMLOperation(declaration.getName(), locationInfo);
  //          umlCompanionObject.addMethod(method);
        }
        return null;
    }

    public void processObject(KtObjectDeclaration objectDeclaration, String filePath) {
        String objectName = objectDeclaration.getName();
        LocationInfo locationInfo =
            generateLocationInfo(objectDeclaration.getContainingKtFile(), filePath, objectDeclaration,
                CodeElementType.TYPE_DECLARATION);
//        UMLClass object =
//            new UMLClass(objectDeclaration.getContainingKtFile().getPackageFqName().asString(), objectName,
//                locationInfo,
//                objectDeclaration.isTopLevel(), processImports(objectDeclaration.getContainingKtFile()));
//        object.setObject(true);
//        object.setVisibility(extractVisibilityModifier(objectDeclaration));

        KtClassBody body = objectDeclaration.getBody();
        if (body != null) {
            List<KtNamedFunction> functions = body.getFunctions();
            for (KtNamedFunction function : functions) {
//                UMLOperation operation =
//                    processMethodDeclaration(objectDeclaration.getContainingKtFile(), function,
//                        object.isInterface(), filePath);
//                operation.setClassName(object.getQualifiedName());
//                object.addOperation(operation);
            }
            List<KtProperty> properties = body.getProperties();
            for (KtProperty property : properties) {
//                UMLAttribute umlAttribute =
//                    processFieldDeclaration(property.getContainingKtFile(), property, filePath);
//                umlAttribute.setClassName(object.getQualifiedName());
//                object.addAttribute(umlAttribute);
            }
        }
//        this.getUmlModel().addClass(object);
    }

    private String extractVisibilityModifier(KtNamedDeclaration ktNamedDeclaration) {
        KtModifierList modifiers = ktNamedDeclaration.getModifierList();
        String visibility = "public";
        if (modifiers != null) {
            if (modifiers.hasModifier(PRIVATE_KEYWORD)) {
                visibility = "private";
            } else if (modifiers.hasModifier(INTERNAL_KEYWORD)) {
                visibility = "internal";
            } else if (modifiers.hasModifier(PROTECTED_KEYWORD)) {
                visibility = "protected";
            }
        }
        return visibility;
    }

    public PsiFile buildPsiFile(String file, KotlinCoreEnvironment environment, String content) throws IOException {
        File newFile = new File("tmp/" + file);
        FileUtilRt.createDirectory(newFile);
        PsiFileFactoryImpl factory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(environment.getProject());
        KotlinLightVirtualFile virtualFile = new KotlinLightVirtualFile(newFile, content);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        return factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false);
    }

    private LocationInfo generateLocationInfo(KtFile ktFile,
                                              String sourceFile,
                                              KtElement node,
                                              CodeElementType codeElementType) {
        return new LocationInfo(ktFile, sourceFile, node, codeElementType);
    }

}
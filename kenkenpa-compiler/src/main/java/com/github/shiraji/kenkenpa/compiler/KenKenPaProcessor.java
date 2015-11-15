package com.github.shiraji.kenkenpa.compiler;

import com.github.shiraji.kenkenpa.annotations.Hop;
import com.github.shiraji.kenkenpa.annotations.Hops;
import com.github.shiraji.kenkenpa.annotations.KenKenPa;
import com.github.shiraji.kenkenpa.annotations.Land;
import com.github.shiraji.kenkenpa.annotations.TakeOff;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static javax.tools.Diagnostic.Kind.ERROR;

@AutoService(Processor.class)
public class KenKenPaProcessor extends AbstractProcessor {

    private static final String GENERATE_CLASSNAME_PREFIX = "KenKenPa_";
    private static final String GENERATE_TAKEOFF_METHOD_PREFIX = "takeOff$$";
    private static final String GENERATE_LAND_METHOD_PREFIX = "land$$";
    private static final String CURRENT_STATE_FIELD_NAME = "$$mCurrentState$$";

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(KenKenPa.class.getName(), Hop.class.getName(),
                Land.class.getName(), TakeOff.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    private Elements mElementUtils;
    private Filer mFiler;
    private Types mTypeUtils;

    private String mDefaultState;
    Set<String> mStates = new HashSet<>();
    Map<String, Element> mLandMap = new HashMap<>();
    Map<String, Element> mTakeOffMap = new HashMap<>();
    Map<Element, Annotation> mHopMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        mElementUtils = env.getElementUtils();
        mTypeUtils = env.getTypeUtils();
        mFiler = env.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(KenKenPa.class);
        for (Element element : elements) {
            TypeElement typeElement = (TypeElement)element;
            setDefaultStateFromTypeElement(typeElement);
            TypeSpec.Builder typeSpecBuilder = createClassTypeSpec(typeElement);
            writeJavaFile(typeElement, typeSpecBuilder);
            clearData();
        }
        return true;
    }

    private boolean hasFromState(String stateName) {
        List<Hop> hops = new LinkedList<>();
        for (Map.Entry<Element, Annotation> entry : mHopMap.entrySet()) {
            hops.addAll(createHopListFromEntry(entry.getValue()));
        }

        List<String> hopFromStates = new ArrayList<>();
        for (Hop hop : hops) {
            hopFromStates.add(hop.from());
        }

        return hopFromStates.contains(stateName);
    }

    private boolean hasToState(String stateName) {
        List<Hop> hops = new LinkedList<>();
        for (Map.Entry<Element, Annotation> entry : mHopMap.entrySet()) {
            hops.addAll(createHopListFromEntry(entry.getValue()));
        }

        List<String> hopToStates = new ArrayList<>();
        for (Hop hop : hops) {
            hopToStates.add(hop.to());
        }

        return hopToStates.contains(stateName);
    }

    private void setDefaultStateFromTypeElement(TypeElement typeElement) {
        KenKenPa annotation = typeElement.getAnnotation(KenKenPa.class);
        mDefaultState = annotation.value();
    }

    private TypeSpec.Builder createClassTypeSpec(TypeElement typeElement) {
        TypeSpec.Builder typeSpecBuilder = createGenerateClassSpec(typeElement);
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() != ElementKind.METHOD) {
                continue;
            }
            addHopMethod(typeSpecBuilder, enclosedElement);
            addHopsMethods(typeSpecBuilder, enclosedElement);
            addLandAnnotationInfo(enclosedElement);
            addTakeOffAnnotationInfo(enclosedElement);
        }

        validateLandAnnotation();
        validateTakeOffAnnotation();

        addGetCurrentStateMethod(typeSpecBuilder, typeElement);
        addGenerateMethods(typeSpecBuilder, typeElement);
        return typeSpecBuilder;
    }

    private void validateTakeOffAnnotation() {
        for (Map.Entry<String, Element> entry : mTakeOffMap.entrySet()) {
            if (!hasFromState(entry.getKey())) {
                logParsingError(entry.getValue(), TakeOff.class,
                        new IllegalArgumentException(String.format("No state %s set Hop(from)", entry.getKey())));
            }
        }
    }

    private void validateLandAnnotation() {
        for (Map.Entry<String, Element> entry : mLandMap.entrySet()) {
            if (!mDefaultState.equals(entry.getKey()) && !hasToState(entry.getKey())) {
                logParsingError(entry.getValue(), Land.class,
                        new IllegalArgumentException(String.format("No state %s set Hop(to)", entry.getKey())));
            }
        }
    }

    private void clearData() {
        mStates.clear();
        mLandMap.clear();
        mTakeOffMap.clear();
        mHopMap.clear();
    }

    private void addGetCurrentStateMethod(TypeSpec.Builder typeSpecBuilder, TypeElement typeElement) {
        for (TypeMirror type : typeElement.getInterfaces()) {
            if ("com.github.shiraji.kenkenpa.interfaces.GetCurrentState".equals(type.toString())) {
                typeSpecBuilder.addMethod(createGetCurrentStateMethod().build());
                return;
            }
        }
    }

    private void writeJavaFile(TypeElement typeElement, TypeSpec.Builder typeSpecBuilder) {
        JavaFile javaFile = JavaFile
                .builder(getPackageName(typeElement), typeSpecBuilder.build())
                .addFileComment("Generated code from Ken-Ken-Pa. Do not modify!")
                .build();
        try {
            javaFile.writeTo(mFiler);
        } catch (IOException e) {
            error(typeElement, "Unable to write Java file for type %s: %s", typeElement,
                    e.getMessage());
        }
    }

    private TypeSpec.Builder createGenerateClassSpec(TypeElement typeElement) {
        return TypeSpec
                .classBuilder(generateClassName(typeElement))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(ClassName.get(typeElement))
                .addField(createCurrentStateField().build());
    }

    private List<MethodSpec> createConstructorSpec(TypeElement typeElement) {
        List<MethodSpec> constructors = new ArrayList<>();
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.CONSTRUCTOR) {
                MethodSpec.Builder constructorMethodSpec = MethodSpec.constructorBuilder();
                ExecutableElement executableElement = (ExecutableElement)enclosedElement;

                List<? extends VariableElement> parameters = executableElement.getParameters();
                if (parameters.size() > 0) {
                    for (VariableElement parameter : parameters) {
                        TypeName type = TypeName.get(parameter.asType());
                        String name = parameter.getSimpleName().toString();
                        Set<Modifier> parameterModifiers = parameter.getModifiers();
                        ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, name)
                                .addModifiers(parameterModifiers.toArray(new Modifier[parameterModifiers.size()]));
                        for (AnnotationMirror mirror : parameter.getAnnotationMirrors()) {
                            parameterBuilder.addAnnotation(AnnotationSpec.get(mirror));
                        }
                        constructorMethodSpec.addParameter(parameterBuilder.build());
                    }

                    constructorMethodSpec.varargs(executableElement.isVarArgs());
                }

                for (TypeMirror thrownType : executableElement.getThrownTypes()) {
                    constructorMethodSpec.addException(TypeName.get(thrownType));
                }

                constructorMethodSpec = constructorMethodSpec.addStatement("super($L)", createSuperMethodParameterString(
                        executableElement));
                constructorMethodSpec.addStatement("this.$N = $S", CURRENT_STATE_FIELD_NAME, mDefaultState);

                if (mLandMap.containsKey(mDefaultState)) {
                    Element defaultLandElement = mLandMap.get(mDefaultState);
                    constructorMethodSpec.addStatement("$L", defaultLandElement);
                }

                constructors.add(constructorMethodSpec.build());
            }
        }

        if (constructors.size() == 0) {
            MethodSpec.Builder constructorMethodSpec = MethodSpec.constructorBuilder();
            constructorMethodSpec.addStatement("this.$N = $S", CURRENT_STATE_FIELD_NAME, mDefaultState);
            constructors.add(constructorMethodSpec.build());
        }

        return constructors;
    }

    private FieldSpec.Builder createCurrentStateField() {
        return FieldSpec.builder(
                ClassName.get(String.class), CURRENT_STATE_FIELD_NAME,
                Modifier.PRIVATE);
    }

    private void addGenerateMethods(TypeSpec.Builder typeSpecBuilder, TypeElement typeElement) {
        typeSpecBuilder.addMethods(createConstructorSpec(typeElement));

        for (Map.Entry<Element, Annotation> entry : mHopMap.entrySet()) {
            MethodSpec.Builder exitMethodSpec = createTakeOffMethod(entry);
            typeSpecBuilder.addMethod(exitMethodSpec.build());

            MethodSpec.Builder enterMethodSpec = createLandMethod(entry);
            typeSpecBuilder.addMethod(enterMethodSpec.build());
        }
    }

    private MethodSpec.Builder createGetCurrentStateMethod() {
        return MethodSpec
                .methodBuilder("getCurrentState")
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $L", CURRENT_STATE_FIELD_NAME);
    }

    private void addHopMethod(TypeSpec.Builder typeSpecBuilder, Element element2) {
        Hop hop = element2.getAnnotation(Hop.class);
        if (hop == null) {
            return;
        }
        mStates.add(hop.from());
        mStates.add(hop.to());
        mHopMap.put(element2, hop);
        MethodSpec.Builder hopMethod = createHopMethod((ExecutableElement)element2);
        typeSpecBuilder.addMethod(hopMethod.build());
    }

    private void addHopsMethods(TypeSpec.Builder typeSpecBuilder, Element element2) {
        Hops hops = element2.getAnnotation(Hops.class);
        if (hops == null) {
            return;
        }
        addHopsToStates(element2, hops);
        mHopMap.put(element2, hops);
        MethodSpec.Builder hopMethod = createHopMethod((ExecutableElement)element2);
        typeSpecBuilder.addMethod(hopMethod.build());
    }

    private void addLandAnnotationInfo(Element element) {
        Land land = element.getAnnotation(Land.class);
        if (land == null) {
            return;
        }

        if (!hasNoParameters(element, Land.class)) {
            return;
        }

        mStates.add(land.value());
        if (mLandMap.containsKey(land.value())) {
            logParsingError(element, Land.class,
                    new IllegalArgumentException(String.format("state %s has multiple @Land", land.value())));
        }
        mLandMap.put(land.value(), element);
    }

    private boolean hasNoParameters(Element element, Class<? extends Annotation> aClass) {
        ExecutableElement executableElement = (ExecutableElement)element;
        List<? extends VariableElement> parameters = executableElement.getParameters();
        if (parameters.size() > 0) {
            logParsingError(element, aClass,
                    new IllegalArgumentException(String.format("%s has too many parameter(s): %s", element.getSimpleName(),
                            parameters.size())));
            return false;
        }
        return true;
    }

    private void addTakeOffAnnotationInfo(Element element) {
        TakeOff takeOff = element.getAnnotation(TakeOff.class);
        if (takeOff == null) {
            return;
        }

        if (!hasNoParameters(element, TakeOff.class)) {
            return;
        }

        mStates.add(takeOff.value());
        if (mTakeOffMap.containsKey(takeOff.value())) {
            logParsingError(element, TakeOff.class,
                    new IllegalArgumentException(String.format("state %s has multiple @TakeOff", takeOff.value())));
        }
        mTakeOffMap.put(takeOff.value(), element);
    }

    private MethodSpec.Builder createLandMethod(Map.Entry<Element, Annotation> entry) {
        List<Hop> hopList = createHopListFromEntry(entry.getValue());
        return createStateLandMethod(entry.getKey(), hopList);
    }

    private MethodSpec.Builder createTakeOffMethod(Map.Entry<Element, Annotation> entry) {
        List<Hop> hopList = createHopListFromEntry(entry.getValue());
        return createStateTakeOffMethod(entry.getKey(), hopList);
    }

    private List<Hop> createHopListFromEntry(Annotation annotation) {
        List<Hop> hopList = new LinkedList<>();
        if (annotation instanceof Hop) {
            hopList.add((Hop)annotation);
        } else if (annotation instanceof Hops) {
            Hops hops = (Hops)annotation;
            hopList.addAll(Arrays.asList(hops.value()));
        }
        return hopList;
    }

    private MethodSpec.Builder createStateLandMethod(Element element, Iterable<Hop> hops) {
        MethodSpec.Builder stateLandMethodSpec = MethodSpec
                .methodBuilder(GENERATE_LAND_METHOD_PREFIX + element.getSimpleName())
                .addModifiers(Modifier.FINAL, Modifier.PRIVATE)
                .addParameter(String.class, "newState");

        stateLandMethodSpec.beginControlFlow("switch($L)", "newState");
        Set<String> handledLandStateSet = new HashSet<>();
        for (Hop hop : hops) {
            if (handledLandStateSet.contains(hop.to())) {
                continue;
            }

            stateLandMethodSpec.addCode("case $S:\n", hop.to());
            if (mLandMap.containsKey(hop.to())) {
                Element fromElement = mLandMap.get(hop.to());
                stateLandMethodSpec.addStatement("$L", fromElement);
            }
            stateLandMethodSpec.addStatement("break");
            handledLandStateSet.add(hop.to());
        }
        stateLandMethodSpec.endControlFlow();
        return stateLandMethodSpec;
    }

    private void addLandMethodCall(MethodSpec.Builder stateLandMethodSpec, Element fromElement, String variableName) {
        StringBuilder sb = new StringBuilder(fromElement.toString());
        int stringParameterIndex = sb.indexOf("java.lang.String");
        if (stringParameterIndex > 0) {
            sb = sb.replace(stringParameterIndex, stringParameterIndex + "java.lang.String".length(), variableName);
        }
        stateLandMethodSpec.addStatement("$L", sb.toString());
    }

    private MethodSpec.Builder createStateTakeOffMethod(Element element, Iterable<Hop> hops) {
        MethodSpec.Builder exitMethodSpec = MethodSpec
                .methodBuilder(GENERATE_TAKEOFF_METHOD_PREFIX + element.getSimpleName().toString())
                .returns(String.class)
                .addModifiers(Modifier.FINAL, Modifier.PRIVATE);

        exitMethodSpec.beginControlFlow("switch($L)", CURRENT_STATE_FIELD_NAME);
        for (Hop hop : hops) {
            exitMethodSpec.addCode("case $S:\n", hop.from());
            if (mTakeOffMap.containsKey(hop.from())) {
                Element element4 = mTakeOffMap.get(hop.from());
                exitMethodSpec.addStatement("$L", element4.toString());
            }
            exitMethodSpec.addStatement("return $S", hop.to());
        }
        exitMethodSpec.endControlFlow();
        exitMethodSpec.addCode("// No definition! Return the default state\n");
        exitMethodSpec.addStatement("return $S", mDefaultState);
        return exitMethodSpec;
    }

    private MethodSpec.Builder createHopMethod(ExecutableElement executableElement) {
        MethodSpec.Builder hopMethodSpec = MethodSpec.overriding(executableElement).addModifiers(Modifier.FINAL);
        hopMethodSpec = hopMethodSpec
                .addStatement("String newState = $L$L()", GENERATE_TAKEOFF_METHOD_PREFIX, executableElement.getSimpleName());

        boolean hasReturnValue = executableElement.getReturnType().getKind() != TypeKind.VOID;
        hopMethodSpec = addSuperMethodCall(executableElement, hopMethodSpec, hasReturnValue);

        hopMethodSpec.addStatement("$L$L($L)", GENERATE_LAND_METHOD_PREFIX, executableElement.getSimpleName(), "newState")
                .addStatement("$L = newState", CURRENT_STATE_FIELD_NAME);

        if (hasReturnValue) {
            hopMethodSpec.addStatement("return returnValue");
        }
        return hopMethodSpec;
    }

    private MethodSpec.Builder addSuperMethodCall(ExecutableElement executableElement, MethodSpec.Builder hopMethodSpec, boolean hasReturnValue) {
        StringBuilder sb = new StringBuilder("super.$L($L)");
        if (hasReturnValue) {
            sb.insert(0, String.format("%s returnValue = ", executableElement.getReturnType().toString()));
        }
        hopMethodSpec = hopMethodSpec.addStatement(sb.toString(), executableElement
                .getSimpleName(), createSuperMethodParameterString(executableElement));
        return hopMethodSpec;
    }

    /**
     * create super method call with parameters.
     *
     * @param executableElement a method that will override
     * @return String that contains super method call
     */
    private String createSuperMethodParameterString(ExecutableElement executableElement) {
        StringBuilder superMethodParameters = new StringBuilder();
        List<? extends VariableElement> parameters = executableElement.getParameters();
        if (parameters.size() > 0) {
            VariableElement parameter = parameters.get(0);
            superMethodParameters.append(parameter.getSimpleName());

            for (int i = 1; i < parameters.size(); i++) {
                superMethodParameters.append(",");
                superMethodParameters.append(parameters.get(i).getSimpleName());
            }
        }
        return superMethodParameters.toString();
    }

    private void addHopsToStates(Element element2, Hops hops) {
        List<String> hopsFrom = new ArrayList<>();
        for (Hop hop : hops.value()) {
            if (hopsFrom.contains(hop.from())) {
                logParsingError(element2, Hops.class, new IllegalArgumentException("'from' must not have the same state."));
            }
            hopsFrom.add(hop.from());
            mStates.add(hop.from());
            mStates.add(hop.to());
        }
    }

    private void logParsingError(Element element,
                                 Class<? extends Annotation> annotation, Exception e) {
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        error(element, "Unable to parse @%s.\n\n%s",
                annotation.getSimpleName(), stackTrace);
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(ERROR, message, element);
    }

    private String getPackageName(TypeElement typeElement) {
        return mElementUtils.getPackageOf(typeElement).getQualifiedName()
                .toString();
    }

    private String generateClassName(TypeElement typeElement) {
        return GENERATE_CLASSNAME_PREFIX + getClassName(typeElement, getPackageName(typeElement));
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen)
                .replace('.', '$');
    }
}

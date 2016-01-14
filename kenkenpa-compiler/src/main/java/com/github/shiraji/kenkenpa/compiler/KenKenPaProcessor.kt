package com.github.shiraji.kenkenpa.compiler

import com.github.shiraji.kenkenpa.annotations.*
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterables
import com.squareup.javapoet.*
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.TypeKind
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.NOTE

//@AutoService(Processor::class)
class KenKenPaProcessor : AbstractProcessor() {

    private var mElementUtils: Elements? = null
    private var mFiler: Filer? = null
    private var mTypeUtils: Types? = null

    private var mDefaultState: String? = null
    internal var mStates: MutableSet<String> = HashSet()
    internal var mLandMap: MutableMap<String, Element> = HashMap()
    internal var mTakeOffMap: MutableMap<String, Element> = HashMap()
    internal var mHopMap: MutableMap<Element, Annotation> = HashMap()

    private val GENERATE_CLASSNAME_PREFIX = "KenKenPa_"
    private val GENERATE_TAKEOFF_METHOD_PREFIX = "takeOff$$"
    private val GENERATE_LAND_METHOD_PREFIX = "land$$"
    private val CURRENT_STATE_FIELD_NAME = "$\$mCurrentState$$"

    override fun getSupportedAnnotationTypes(): Set<String?> =
            setOf(KenKenPa::class.qualifiedName, Hop::class.qualifiedName, Land::class.qualifiedName, TakeOff::class.qualifiedName)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()


    override fun init(env: ProcessingEnvironment) {
        super.init(env)

        mElementUtils = env.elementUtils
        mTypeUtils = env.typeUtils
        mFiler = env.filer
    }

    override fun process(annotations: Set<TypeElement>,
                         roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(KenKenPa::class.java).forEach {
            setDefaultStateFromTypeElement(it as TypeElement)
            val typeSpecBuilder = createClassTypeSpec(it)
            writeJavaFile(it, typeSpecBuilder)
            clearData()
        }
        return true
    }

    private fun hasFromState(stateName: String): Boolean {
        val hops = LinkedList<Hop>()
        for (entry in mHopMap.entries) {
            hops.addAll(createHopListFromEntry(entry.value))
        }

        val hopFromStates = ArrayList<String>()
        for (hop in hops) {
            hopFromStates.add(hop.from)
        }

        return hopFromStates.contains(stateName)
    }

    private fun hasToState(stateName: String): Boolean {
        val hops = LinkedList<Hop>()
        for (entry in mHopMap.entries) {
            hops.addAll(createHopListFromEntry(entry.value))
        }

        val hopToStates = ArrayList<String>()
        for (hop in hops) {
            hopToStates.add(hop.to)
        }

        return hopToStates.contains(stateName)
    }

    private fun setDefaultStateFromTypeElement(typeElement: TypeElement) {
        val annotation = typeElement.getAnnotation<KenKenPa>(KenKenPa::class.java)
        mDefaultState = annotation.value
    }

    private fun createClassTypeSpec(typeElement: TypeElement): TypeSpec.Builder {
        val typeSpecBuilder = createGenerateClassSpec(typeElement)
        for (enclosedElement in typeElement.enclosedElements) {
            if (enclosedElement.kind != ElementKind.METHOD) {
                continue
            }
            addHopMethod(typeSpecBuilder, enclosedElement)
            addHopsMethods(typeSpecBuilder, enclosedElement)
            addLandAnnotationInfo(enclosedElement)
            addTakeOffAnnotationInfo(enclosedElement)
        }
        addGetCurrentStateMethod(typeSpecBuilder, typeElement)
        addGenerateMethods(typeSpecBuilder, typeElement)
        return typeSpecBuilder
    }

    private fun clearData() {
        mStates.clear()
        mLandMap.clear()
        mTakeOffMap.clear()
        mHopMap.clear()
    }

    private fun addGetCurrentStateMethod(typeSpecBuilder: TypeSpec.Builder, typeElement: TypeElement) {
        for (type in typeElement.interfaces) {
            if ("com.github.shiraji.kenkenpa.interfaces.GetCurrentState" == type.toString()) {
                typeSpecBuilder.addMethod(createGetCurrentStateMethod().build())
                return
            }
        }
    }

    private fun writeJavaFile(typeElement: TypeElement, typeSpecBuilder: TypeSpec.Builder) {
        val javaFile = JavaFile.builder(getPackageName(typeElement), typeSpecBuilder.build()).addFileComment("Generated code from Ken-Ken-Pa. Do not modify!").build()
        try {
            javaFile.writeTo(mFiler)
        } catch (e: IOException) {
            error(typeElement, "Unable to write Java file for type %s: %s", typeElement,
                    e.message.toString())
        }

    }

    private fun createGenerateClassSpec(typeElement: TypeElement): TypeSpec.Builder {
        return TypeSpec.classBuilder(generateClassName(typeElement)).addModifiers(Modifier.PUBLIC, Modifier.FINAL).superclass(ClassName.get(typeElement)).addField(createCurrentStateField().build())
    }

    private fun createConstructorSpec(typeElement: TypeElement): List<MethodSpec> {
        val constructors = ArrayList<MethodSpec>()
        for (enclosedElement in typeElement.enclosedElements) {
            if (enclosedElement.kind == ElementKind.CONSTRUCTOR) {
                var constructorMethodSpec: MethodSpec.Builder = MethodSpec.constructorBuilder()
                val executableElement = enclosedElement as ExecutableElement

                val parameters = executableElement.parameters
                if (parameters.size > 0) {
                    for (parameter in parameters) {
                        val type = TypeName.get(parameter.asType())
                        val name = parameter.simpleName.toString()
                        val parameterModifiers = parameter.modifiers
                        val parameterBuilder = ParameterSpec.builder(type, name).addModifiers(*Iterables.toArray(parameterModifiers, Modifier::class.java))
                        for (mirror in parameter.annotationMirrors) {
                            parameterBuilder.addAnnotation(AnnotationSpec.get(mirror))
                        }
                        constructorMethodSpec.addParameter(parameterBuilder.build())
                    }

                    constructorMethodSpec.varargs(executableElement.isVarArgs)
                }

                for (thrownType in executableElement.thrownTypes) {
                    constructorMethodSpec.addException(TypeName.get(thrownType))
                }

                constructorMethodSpec = constructorMethodSpec.addStatement("super(\$L)", createSuperMethodParameterString(
                        executableElement))
                constructorMethodSpec.addStatement("this.\$N = \$S", CURRENT_STATE_FIELD_NAME, mDefaultState)

                if (mLandMap.containsKey(mDefaultState as String)) {
                    val defaultLandElement = mLandMap.get(mDefaultState as String)
                    constructorMethodSpec.addStatement("\$L", defaultLandElement)
                }

                constructors.add(constructorMethodSpec.build())
            }
        }

        if (constructors.size == 0) {
            val constructorMethodSpec = MethodSpec.constructorBuilder()
            constructorMethodSpec.addStatement("this.\$N = \$S", CURRENT_STATE_FIELD_NAME, mDefaultState)
            constructors.add(constructorMethodSpec.build())
        }

        return constructors
    }

    private fun createCurrentStateField(): FieldSpec.Builder {
        return FieldSpec.builder(
                ClassName.get(String::class.java), CURRENT_STATE_FIELD_NAME,
                Modifier.PRIVATE)
    }

    private fun addGenerateMethods(typeSpecBuilder: TypeSpec.Builder, typeElement: TypeElement) {
        typeSpecBuilder.addMethods(createConstructorSpec(typeElement))

        for (entry in mHopMap.entries) {
            val exitMethodSpec = createTakeOffMethod(entry)
            typeSpecBuilder.addMethod(exitMethodSpec.build())

            val enterMethodSpec = createLandMethod(entry)
            typeSpecBuilder.addMethod(enterMethodSpec.build())
        }
    }

    private fun createGetCurrentStateMethod(): MethodSpec.Builder {
        return MethodSpec.methodBuilder("getCurrentState").addModifiers(Modifier.FINAL, Modifier.PUBLIC).addAnnotation(Override::class.java).returns(String::class.java).addStatement("return \$L", CURRENT_STATE_FIELD_NAME)
    }

    private fun addHopMethod(typeSpecBuilder: TypeSpec.Builder, element2: Element) {
        val hop = element2.getAnnotation<Hop>(Hop::class.java) ?: return
        mStates.add(hop.from)
        mStates.add(hop.to)
        mHopMap.put(element2, hop)
        val hopMethod = createHopMethod(element2 as ExecutableElement)
        typeSpecBuilder.addMethod(hopMethod.build())
    }

    private fun addHopsMethods(typeSpecBuilder: TypeSpec.Builder, element2: Element) {
        val hops = element2.getAnnotation<Hops>(Hops::class.java) ?: return
        addHopsToStates(element2, hops)
        mHopMap.put(element2, hops)
        val hopMethod = createHopMethod(element2 as ExecutableElement)
        typeSpecBuilder.addMethod(hopMethod.build())
    }

    private fun addLandAnnotationInfo(element: Element) {
        val land = element.getAnnotation<Land>(Land::class.java) ?: return

        if (!hasNoParameters(element, Land::class.java)) {
            return
        }

        mStates.add(land.value)
        if (mLandMap.containsKey(land.value)) {
            logParsingError(element, Land::class.java,
                    IllegalArgumentException("state %s has multiple @Land".format(land.value)))
        }

        if (mDefaultState != land.value && !hasToState(land.value)) {
            logParsingError(element, Land::class.java,
                    IllegalArgumentException("No state %s set Hop(to)".format(land.value)))
        }

        mLandMap.put(land.value, element)
    }

    private fun hasNoParameters(element: Element, aClass: Class<out Annotation>): Boolean {
        val executableElement = element as ExecutableElement
        val parameters = executableElement.parameters
        if (parameters.size > 0) {
            logParsingError(element, aClass,
                    IllegalArgumentException("%s has too many parameter(s): %s".format(element.simpleName, parameters.size)))
            return false
        }
        return true
    }

    private fun addTakeOffAnnotationInfo(element: Element) {
        val takeOff = element.getAnnotation<TakeOff>(TakeOff::class.java) ?: return

        if (!hasNoParameters(element, TakeOff::class.java)) {
            return
        }

        mStates.add(takeOff.value)
        if (mTakeOffMap.containsKey(takeOff.value)) {
            logParsingError(element, TakeOff::class.java,
                    IllegalArgumentException("state %s has multiple @TakeOff".format(takeOff.value)))
        }

        if (!hasFromState(takeOff.value)) {
            logParsingError(element, TakeOff::class.java,
                    IllegalArgumentException("No state %s set Hop(from)".format(takeOff.value)))
        }

        mTakeOffMap.put(takeOff.value, element)
    }

    private fun createLandMethod(entry: Map.Entry<Element, Annotation>): MethodSpec.Builder {
        val hopList = createHopListFromEntry(entry.value)
        return createStateLandMethod(entry.key, hopList)
    }

    private fun createTakeOffMethod(entry: Map.Entry<Element, Annotation>): MethodSpec.Builder {
        val hopList = createHopListFromEntry(entry.value)
        return createStateTakeOffMethod(entry.key, hopList)
    }

    private fun createHopListFromEntry(annotation: Annotation): List<Hop> {
        val hopList = LinkedList<Hop>()
        if (annotation is Hop) {
            hopList.add(annotation)
        } else if (annotation is Hops) {
            hopList.addAll(Arrays.asList<Hop>(*annotation.value))
        }
        return hopList
    }

    private fun createStateLandMethod(element: Element, hops: Iterable<Hop>): MethodSpec.Builder {
        val stateLandMethodSpec = MethodSpec.methodBuilder(GENERATE_LAND_METHOD_PREFIX + element.simpleName).addModifiers(Modifier.FINAL, Modifier.PRIVATE).addParameter(String::class.java, "newState")

        stateLandMethodSpec.beginControlFlow("switch(\$L)", "newState")
        val handledLandStateSet = HashSet<String>()
        for (hop in hops) {
            if (handledLandStateSet.contains(hop.to)) {
                continue
            }

            stateLandMethodSpec.addCode("case \$S:\n", hop.to)
            if (mLandMap.containsKey(hop.to)) {
                val fromElement = mLandMap[hop.to]
                stateLandMethodSpec.addStatement("\$L", fromElement)
            }
            stateLandMethodSpec.addStatement("break")
            handledLandStateSet.add(hop.to)
        }
        stateLandMethodSpec.endControlFlow()
        return stateLandMethodSpec
    }

    private fun addLandMethodCall(stateLandMethodSpec: MethodSpec.Builder, fromElement: Element, variableName: String) {
        var sb = StringBuilder(fromElement.toString())
        val stringParameterIndex = sb.indexOf("java.lang.String")
        if (stringParameterIndex > 0) {
            sb = sb.replace(stringParameterIndex, stringParameterIndex + "java.lang.String".length, variableName)
        }
        stateLandMethodSpec.addStatement("\$L", sb.toString())
    }

    private fun createStateTakeOffMethod(element: Element, hops: Iterable<Hop>): MethodSpec.Builder {
        val exitMethodSpec = MethodSpec.methodBuilder(GENERATE_TAKEOFF_METHOD_PREFIX + element.simpleName.toString()).returns(String::class.java).addModifiers(Modifier.FINAL, Modifier.PRIVATE)

        exitMethodSpec.beginControlFlow("switch(\$L)", CURRENT_STATE_FIELD_NAME)
        for (hop in hops) {
            exitMethodSpec.addCode("case \$S:\n", hop.from)
            if (mTakeOffMap.containsKey(hop.from)) {
                val element4 = mTakeOffMap[hop.from]
                exitMethodSpec.addStatement("\$L", element4.toString())
            }
            exitMethodSpec.addStatement("return \$S", hop.to)
        }
        exitMethodSpec.endControlFlow()
        exitMethodSpec.addCode("// No definition! Return the default state\n")
        exitMethodSpec.addStatement("return \$S", mDefaultState)
        return exitMethodSpec
    }

    private fun createHopMethod(executableElement: ExecutableElement): MethodSpec.Builder {
        var hopMethodSpec: MethodSpec.Builder = MethodSpec.overriding(executableElement).addModifiers(Modifier.FINAL)
        hopMethodSpec = hopMethodSpec.addStatement("String newState = \$L\$L()", GENERATE_TAKEOFF_METHOD_PREFIX, executableElement.simpleName)

        val hasReturnValue = executableElement.returnType.kind != TypeKind.VOID
        hopMethodSpec = addSuperMethodCall(executableElement, hopMethodSpec, hasReturnValue)

        hopMethodSpec.addStatement("\$L\$L(\$L)", GENERATE_LAND_METHOD_PREFIX, executableElement.simpleName, "newState").addStatement("\$L = newState", CURRENT_STATE_FIELD_NAME)

        if (hasReturnValue) {
            hopMethodSpec.addStatement("return returnValue")
        }
        return hopMethodSpec
    }

    private fun addSuperMethodCall(executableElement: ExecutableElement, hopMethodSpec: MethodSpec.Builder, hasReturnValue: Boolean): MethodSpec.Builder {
        var hopMethodSpec = hopMethodSpec
        val sb = StringBuilder("super.\$L(\$L)")
        if (hasReturnValue) {
            sb.insert(0, "%s returnValue = ".format(executableElement.returnType.toString()))
        }
        hopMethodSpec = hopMethodSpec.addStatement(sb.toString(), executableElement.simpleName, createSuperMethodParameterString(executableElement))
        return hopMethodSpec
    }

    /**
     * create super method call with parameters.

     * @param executableElement a method that will override
     * *
     * @return String that contains super method call
     */
    private fun createSuperMethodParameterString(executableElement: ExecutableElement): String {
        val superMethodParameters = StringBuilder()
        val parameters = executableElement.parameters
        if (parameters.size > 0) {
            val parameter = parameters[0]
            superMethodParameters.append(parameter.simpleName)

            for (i in 1..parameters.size - 1) {
                superMethodParameters.append(",")
                superMethodParameters.append(parameters[i].simpleName)
            }
        }
        return superMethodParameters.toString()
    }

    private fun addHopsToStates(element2: Element, hops: Hops) {
        val hopsFrom = ArrayList<String>()
        for (hop in hops.value) {
            if (hopsFrom.contains(hop.from)) {
                logParsingError(element2, Hops::class.java, IllegalArgumentException("'from' must not have the same state."))
            }
            hopsFrom.add(hop.from)
            mStates.add(hop.from)
            mStates.add(hop.to)
        }
    }

    private fun logParsingError(element: Element,
                                annotation: Class<out Annotation>, e: Exception) {
        val stackTrace = StringWriter()
        e.printStackTrace(PrintWriter(stackTrace))
        error(element, "Unable to parse @%s.",//\n\n%s",
                annotation.simpleName)//, stackTrace.toString())
    }

    private fun error(element: Element, message: String, vararg args: Any) {
        var message = message
        if (args.size() > 0) {
            message = message.format(args)
        }
        processingEnv.messager.printMessage(ERROR, message, element)
    }

    private fun getPackageName(typeElement: TypeElement): String {
        return mElementUtils!!.getPackageOf(typeElement).qualifiedName.toString()
    }

    private fun generateClassName(typeElement: TypeElement): String {
        return GENERATE_CLASSNAME_PREFIX + getClassName(typeElement, getPackageName(typeElement))
    }

    private fun getClassName(type: TypeElement, packageName: String): String {
        val packageLen = packageName.length + 1
        return type.qualifiedName.toString().substring(packageLen).replace('.', '$')
    }

}

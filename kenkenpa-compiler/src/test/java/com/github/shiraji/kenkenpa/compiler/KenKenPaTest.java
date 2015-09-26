package com.github.shiraji.kenkenpa.compiler;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.tools.JavaFileObject;

import static com.github.shiraji.kenkenpa.compiler.KenKenPaTestHelper.compareSourceCodesWithoutError;

@RunWith(JUnit4.class)
public class KenKenPaTest extends TestCase {

    @Test
    public void onlyKenKenPaAnnotationCreateUselessClassButNoError() {
        // @formatter:off
        JavaFileObject source = JavaFileObjects.forSourceString("test.SimpleFSM", Joiner.on('\n').join(
            "package test;",
            "import com.github.shiraji.kenkenpa.annotations.KenKenPa;",
            "@KenKenPa(\"CIRCLE1\")",
            "public abstract class SimpleFSM {",
            "}"
        ));

        JavaFileObject expectedSource = JavaFileObjects.forSourceString("test.KenKenPa_SimpleFSM", Joiner.on('\n').join(
            "package test;",
            "import java.lang.String;",
            "public final class KenKenPa_SimpleFSM extends SimpleFSM {",
            "  private String $$mCurrentState$$;",
            "  KenKenPa_SimpleFSM() {",
            "    super();",
            "    this.$$mCurrentState$$ = \"CIRCLE1\";",
            "  }",
            "}"
        ));
        // @formatter:on
        compareSourceCodesWithoutError(source, expectedSource);
    }

    @Test
    public void createFSMWithNonDefaultConstructors() {
        // @formatter:off
        JavaFileObject source = JavaFileObjects.forSourceString("test.SimpleFSM", Joiner.on('\n').join(
            "package test;",
            "import com.github.shiraji.kenkenpa.annotations.KenKenPa;",
            "@KenKenPa(\"CIRCLE1\")",
            "public abstract class SimpleFSM {",
            "    private String mCurrentState;",
            "    private int mInt;",
            "    private Object mObject;",
            "    public SimpleFSM(String currentState) {",
            "        mCurrentState = currentState;",
            "    }",
            "    public SimpleFSM(String currentState, int anInt) {",
            "        mCurrentState = currentState;",
            "        mInt = anInt;",
            "    }",
            "    public SimpleFSM(String currentState, int anInt, Object object) {",
            "        mCurrentState = currentState;",
            "        mInt = anInt;",
            "        mObject = object;",
            "    }",
            "}"
        ));

        JavaFileObject expectedSource = JavaFileObjects.forSourceString("test.KenKenPa_SimpleFSM", Joiner.on('\n').join(
            "package test;",
            "import java.lang.Object;",
            "import java.lang.String;",
            "public final class KenKenPa_SimpleFSM extends SimpleFSM {",
            "  private String $$mCurrentState$$;",
            "  KenKenPa_SimpleFSM(String currentState) {",
            "    super(currentState);",
            "    this.$$mCurrentState$$ = \"CIRCLE1\";",
            "  }",
            "  KenKenPa_SimpleFSM(String currentState, int anInt) {",
            "    super(currentState,anInt);",
            "    this.$$mCurrentState$$ = \"CIRCLE1\";",
            "  }",
            "  KenKenPa_SimpleFSM(String currentState, int anInt, Object object) {",
            "    super(currentState,anInt,object);",
            "    this.$$mCurrentState$$ = \"CIRCLE1\";",
            "  }",
            "}"
        ));
        // @formatter:on
        compareSourceCodesWithoutError(source, expectedSource);
    }


}
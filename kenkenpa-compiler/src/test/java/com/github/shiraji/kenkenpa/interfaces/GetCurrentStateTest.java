package com.github.shiraji.kenkenpa.interfaces;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.tools.JavaFileObject;

import static com.github.shiraji.kenkenpa.compiler.KenKenPaTestHelper.compareSourceCodesWithoutError;

@RunWith(JUnit4.class)
public class GetCurrentStateTest extends TestCase {

    @Test
    public void onlyKenKenPaAnnotationCreateUselessClassButNoError() {
        // @formatter:off
        JavaFileObject source = JavaFileObjects.forSourceString("test.SimpleFSM", Joiner.on('\n').join(
            "package test;",
            "import com.github.shiraji.kenkenpa.annotations.KenKenPa;",
            "import com.github.shiraji.kenkenpa.interfaces.GetCurrentState;",
            "@KenKenPa(\"CIRCLE1\")",
            "public abstract class SimpleFSM implements GetCurrentState {",
            "}"
        ));

        JavaFileObject expectedSource = JavaFileObjects.forSourceString("test.KenKenPa_SimpleFSM", Joiner.on('\n').join(
            "package test;",
            "import java.lang.Override;",
            "import java.lang.String;",
            "public final class KenKenPa_SimpleFSM extends SimpleFSM {",
            "  private String $$mCurrentState$$;",
            "  KenKenPa_SimpleFSM() {",
            "    super();",
            "    this.$$mCurrentState$$ = \"CIRCLE1\";",
            "  }",
            "  @Override",
            "  public final String getCurrentState() {",
            "    return $$mCurrentState$$;",
            "  }",
            "}"
        ));
        // @formatter:on
        compareSourceCodesWithoutError(source, expectedSource);
    }
}

package com.github.shiraji.kenkenpa.compiler;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.tools.JavaFileObject;

import static com.github.shiraji.kenkenpa.compiler.KenKenPaTestHelper.compareSourceCodesWithoutError;
import static com.github.shiraji.kenkenpa.compiler.KenKenPaTestHelper.compileShouldFail;

@RunWith(JUnit4.class)
public class HopsTest extends TestCase {
    @Test
    public void simpleSingleUseTest() {
        // @formatter:off
        JavaFileObject source = JavaFileObjects.forSourceString("test.SimpleFSM", Joiner.on('\n').join(
            "package test;",
            "import com.github.shiraji.kenkenpa.annotations.Hop;",
            "import com.github.shiraji.kenkenpa.annotations.Hops;",
            "import com.github.shiraji.kenkenpa.annotations.KenKenPa;",
            "@KenKenPa(\"CIRCLE1\")",
            "public abstract class SimpleFSM {",
            "  @Hops({@Hop(from = \"CIRCLE1\", to = \"CIRCLE2\"), @Hop(from = \"CIRCLE2\", to = \"CIRCLE1\")})",
            "  public void fire() {",
            "    System.out.println(\"fire!\");",
            "  }",
            "}"
        ));

        JavaFileObject expectedSource = JavaFileObjects.forSourceString("test.KenKenPa_SimpleFSM", Joiner.on('\n').join(
            "package test;",
            "import com.github.shiraji.kenkenpa.annotations.Hop;",
            "import com.github.shiraji.kenkenpa.annotations.Hops;",
            "import java.lang.Override;",
            "import java.lang.String;",
            "public final class KenKenPa_SimpleFSM extends SimpleFSM {",
            "  private String $$mCurrentState$$;",
            "  KenKenPa_SimpleFSM() {",
            "    super();",
            "    this.$$mCurrentState$$ = \"CIRCLE1\";",
            "  }",
            "  @Override",
            "  @Hops({@Hop(from = \"CIRCLE1\", to = \"CIRCLE2\"), @Hop(from = \"CIRCLE2\", to = \"CIRCLE1\")})",
            "  public final void fire() {",
            "    String newState = takeOff$$fire();",
            "    super.fire();",
            "    land$$fire(newState);",
            "    $$mCurrentState$$ = newState;",
            "  }",
            "  private final String takeOff$$fire() {",
            "    switch($$mCurrentState$$) {",
            "      case \"CIRCLE1\":",
            "      return \"CIRCLE2\";",
            "      case \"CIRCLE2\":",
            "      return \"CIRCLE1\";",
            "    }",
            "    // No definition! Return the default state",
            "    return \"CIRCLE1\";",
            "  }",
            "  private final void land$$fire(String newState) {",
            "    switch(newState) {",
            "      case \"CIRCLE2\":",
            "      break;",
            "      case \"CIRCLE1\":",
            "      break;",
            "    }",
            "  }",
            "}"
        ));
        // @formatter:on

        compareSourceCodesWithoutError(source, expectedSource);
    }

    @Test
    public void hopsWithSameFromShouldFailCompile() {
        // @formatter:off
        JavaFileObject source = JavaFileObjects.forSourceString("test.SimpleFSM", Joiner.on('\n').join(
            "package test;",
            "import com.github.shiraji.kenkenpa.annotations.Hop;",
            "import com.github.shiraji.kenkenpa.annotations.Hops;",
            "import com.github.shiraji.kenkenpa.annotations.KenKenPa;",
            "import com.github.shiraji.kenkenpa.annotations.TakeOff;",
            "@KenKenPa(\"CIRCLE1\")",
            "public abstract class SimpleFSM {",
            "  @Hops({@Hop(from = \"CIRCLE1\", to = \"CIRCLE2\"), @Hop(from = \"CIRCLE1\", to = \"CIRCLE1\")})",
            "  public void fire() {",
            "    System.out.println(\"fire!\");",
            "  }",
            "}"
        ));
        // @formatter:on
        compileShouldFail(source);
    }

    @Test
    public void hopsAndHopAtSameMethodShouldFailCompile() {
        // @formatter:off
        JavaFileObject source = JavaFileObjects.forSourceString("test.SimpleFSM", Joiner.on('\n').join(
            "package test;",
            "import com.github.shiraji.kenkenpa.annotations.Hop;",
            "import com.github.shiraji.kenkenpa.annotations.Hops;",
            "import com.github.shiraji.kenkenpa.annotations.KenKenPa;",
            "import com.github.shiraji.kenkenpa.annotations.TakeOff;",
            "@KenKenPa(\"CIRCLE1\")",
            "public abstract class SimpleFSM {",
            "  @Hops({@Hop(from = \"CIRCLE1\", to = \"CIRCLE2\"), @Hop(from = \"CIRCLE1\", to = \"CIRCLE1\")})",
            "  @Hop(from = \"CIRCLE3\", to = \"CIRCLE1\")",
            "  public void fire() {",
            "    System.out.println(\"fire!\");",
            "  }",
            "}"
        ));
        // @formatter:on
        compileShouldFail(source);
    }
}

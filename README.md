# Ken-Ken-Pa

[![Build Status](https://secure.travis-ci.org/shiraji/kenkenpa.png)](http://travis-ci.org/shiraji/kenkenpa)  [![Software License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](https://github.com/shiraji/kenkenpa/blob/master/LICENSE)

* [![Download](https://api.bintray.com/packages/shiraji/maven/kenkenpa/images/download.svg)](https://bintray.com/shiraji/maven/kenkenpa/_latestVersion) : kenkenpa
* [![Download](https://api.bintray.com/packages/shiraji/maven/kenkenpa-compiler/images/download.svg) ](https://bintray.com/shiraji/maven/kenkenpa-compiler/_latestVersion) : kenkenpa-compiler

Yet, another light weight Java FSM library. This library bollows the idea from [Google AutoValue](https://github.com/google/auto/tree/master/value). It generates a subclass that handles states.

This is still alpha version.

# What is Ken-Ken-Pa?

Ken-Ken-Pa is a Japanese style of Hop Scotch. The difference between Hop Scotch and Ken-Ken-Pa is whether they use squares or circles.

# How to install?

Use gradle.

```gradle
buildscript {
    repositories {
      mavenCentral()
    }
    dependencies {
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.7'
    }
}

apply plugin: 'com.neenbedankt.android-apt'

android {
    packagingOptions {
        exclude 'META-INF/services/javax.annotation.processing.Processor'
    }
}

dependencies {
    compile 'com.github.shiraji:kenkenpa:0.0.4'
    apt 'com.github.shiraji:kenkenpa-compiler:0.0.4'
}
```

`packagingOptions` block is for developers who use other apt libraries.

If you cannot use gradle, then download jar from Download button on top of the documentation .

# How to use?

There are a few steps to use this library.

First, add `@KenKenPa` annotation to the abstract class. This abstract class will be a state machine class. `@KenKenPa` annotation needs to have a default state. Currently, states are represented by only String values.

```java
@KenKenPa("STATE1")
public abstract class SimpleFSM {
}
```

Secondly, create new instance method. The method must return an instance of subclass. The subclass name format is KenKenPa_XXX where XXX is the abstract class name.

```java
@KenKenPa("STATE1")
public abstract class SimpleFSM {
    public static SimpleFSM create() {
        return new KenKenPa_SimpleFSM();
    }
}
```

Thirdly, add `@Hop` to define state changes.

```java
@KenKenPa("STATE1")
public abstract class SimpleFSM {
    public static SimpleFSM create() {
        return new KenKenPa_SimpleFSM();
    }

    @Hop(from = "STATE1", to = "STATE2")
    public void fire() {
      System.out.println("fire!");
    }
}

```

Now, you are set. You can use this class

```java
SimpleFSM simpleFSM = SimpleFSM.create();
simpleFSM.fire(); // => fire! and change current state to STATE2
```

## How to create multiple hops?

Sadly, there is limitation on Java (less than Java8). You cannot set same annotation on the same method. Instead of using `@Hop`, use `@Hops` which take multiple `@Hop` as parameters.

```java
@KenKenPa("STATE1")
public abstract class SimpleFSM {
    public static SimpleFSM create() {
        return new KenKenPa_SimpleFSM();
    }

    @Hops({@Hop(from = "STATE1", to = "STATE2"),
            @Hop(from = "STATE2", to = "STATE3")})
    public void fire() {
      System.out.println("fire!");
    }
}
```

```java
SimpleFSM simpleFSM = SimpleFSM.create();
simpleFSM.fire(); // => fire!
simpleFSM.fire(); // => fire! and change current state to STATE3
```

## How to get current state?

To get current state, you can add GetCurrentState interface to the abstract class.

```java
@KenKenPa("STATE1")
public abstract class SimpleFSM implements GetCurrentState
```

this interface offers `String getCurrentState()` method.

```java
SimpleFSM simpleFSM = SimpleFSM.create();
simpleFSM.getCurrentState() // => STATE1
```

## What is `@TakeOff`?

When children hop to another circle, they "take off" the current circle. `@TakeOff` is an annotation that represents "run this method when the current state changed from this state." This annocation is useful when the state require clean up.

```java
@KenKenPa("STATE1")
public abstract class MainFSM implements GetCurrentState {
    public static MainFSM newInstance() {
        return new KenKenPa_MainFSM();
    }

    @Hop(from = "STATE1", to = "STATE2")
    public void state1ToState2() {
    }

    @Hop(from = "STATE1", to = "STATE3")
    public void state1ToState3() {
    }

    @Hop(from = "STATE2", to = "STATE1")
    public void state2ToState1() {
    }

    @TakeOff("STATE1")
    void endState1() {
      System.out.println("Exit from STATE1");
    }
}
```

```java
SimpleFSM simpleFSM = SimpleFSM.create();
simpleFSM.state1ToState2(); // => display 'Exit from STATE1'
simpleFSM.state2ToState1(); // => change current state to STATE1
simpleFSM.state1ToState3(); // => display 'Exit from STATE1' and change current state to STATE3
```

Add description for string parameter.

## What is `@Land`?

When children hop to another circle, they 'land' the next circle. `@Land` is an annotation that represents "Run this method when the current state became this state." This annotation is useful when the state have the same initialization steps.

```java
@KenKenPa("STATE1")
public abstract class MainFSM implements GetCurrentState {
    public static MainFSM newInstance() {
        return new KenKenPa_MainFSM();
    }

    @Hop(from = "STATE1", to = "STATE2")
    public void state1ToState2() {
    }

    @Hop(from = "STATE1", to = "STATE3")
    public void state1ToState3() {
    }

    @Hop(from = "STATE2", to = "STATE1")
    public void state2ToState1() {
    }

    @Land("STATE2")
    void startState2() {
      System.out.println("Now STATE2");
    }
}
```

```java
SimpleFSM simpleFSM = SimpleFSM.create();
simpleFSM.state1ToState2(); // => display 'Now STATE2'
simpleFSM.state2ToState1(); // => change current state to STATE1
```

Add description for string parameter.

##TODO

1. Create unit test
1. Accept State as an Object other than String
1. Java6? (this may related to above)
1. Handling unexpected cases
1. Sample codes (Android and Java)

## How actually works?

This is the image of execution step.

![execution_image](website/images/execution_image.png)

If the developer create following KenKenPa annotation class

```java
@KenKenPa("STATE1")
public abstract class TestSM implements GetCurrentState {

    private String mText;

    TestSM(String text) {
        mText = text;
    }

    public static TestSM create(String text) {
        return new KenKenPa_TestSM(text);
    }

    @Hops({@Hop(from = "STATE1", to = "STATE2"), @Hop(from = "STATE2", to = "STATE1")})
    public void fire() {
        System.out.println("Fire!");
    }

    @Hop(from = "STATE1", to = "STATE2")
    public int fire2() {
        return 1;
    }

    @Land("STATE1")
    public void land() {
        System.out.println("land");
    }

    @TakeOff("STATE2")
    public void takeOff() {
        System.out.println("takeoff");
    }
}
```

KenKenPa_TestSM is generated at compile time.

```java
public final class KenKenPa_TestSM extends TestSM {
  private String mCurrentState;

  KenKenPa_TestSM(String text) {
    super(text);
    this.mCurrentState = "STATE1";
  }

  @Override
  @Hops({
      @Hop(from = "STATE1", to = "STATE2"),
      @Hop(from = "STATE2", to = "STATE1")
  })
  public final void fire() {
    String newState = takeOff$$fire();
    super.fire();
    land$$fire(mCurrentState);
    mCurrentState = newState;
  }

  @Override
  @Hop(
      from = "STATE1",
      to = "STATE2"
  )
  public final int fire2() {
    String newState = takeOff$$fire2();
    int returnValue = super.fire2();
    land$$fire2(mCurrentState);
    mCurrentState = newState;
    return returnValue;
  }

  @Override
  public final String getCurrentState() {
    return mCurrentState;
  }

  private final String takeOff$$fire() {
    switch(mCurrentState) {
      case "STATE1":
      return "STATE2";
      case "STATE2":
      takeOff();
      return "STATE1";
    }
    // No definition! Return the default state
    return "STATE1";
  }

  private final void land$$fire(String newState) {
    switch(newState) {
      case "STATE1":
      land();
      break;
      case "STATE2":
      break;
    }
  }

  private final String takeOff$$fire2() {
    switch(mCurrentState) {
      case "STATE1":
      return "STATE2";
    }
    // No definition! Return the default state
    return "STATE1";
  }

  private final void land$$fire2(String newState) {
    switch(newState) {
      case "STATE1":
      land();
      break;
    }
  }
}
```

## Contributing

Contribution is welcome! You may notice but I am not good English writer. My documentation might confuse this library. Please give me pull request to fix documentation. Raise issue is also welcome. Here is how you can contribute this library.

1. Fork it!
1. Clone your forked repository: `git clone your-repositity-url`
1. Create your feature branch: `git checkout -b my-new-feature` (You can skip this step and work on master.)
1. Commit your changes: `git commit -am 'Add some feature'`
1. Push to the branch: `git push origin my-new-feature`
1. Submit a pull request

## License

```
Copyright 2015 Yoshinori Isogai

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

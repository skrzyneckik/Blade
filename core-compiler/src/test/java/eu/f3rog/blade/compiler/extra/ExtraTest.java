package eu.f3rog.blade.compiler.extra;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import org.junit.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import blade.Blade;
import blade.Extra;
import eu.f3rog.blade.compiler.BaseTest;
import eu.f3rog.blade.compiler.BladeProcessor;
import eu.f3rog.blade.compiler.ErrorMsg;
import eu.f3rog.blade.core.BundleWrapper;
import eu.f3rog.blade.core.Bundler;
import eu.f3rog.blade.core.Weave;

import static eu.f3rog.blade.compiler.util.File.file;
import static eu.f3rog.blade.compiler.util.File.generatedFile;


/**
 * Class {@link ExtraTest}
 *
 * @author FrantisekGazo
 */
public final class ExtraTest extends BaseTest {

    @Test
    public void invalidClass() {
        JavaFileObject input = file("com.example", "MainActivity")
                .imports(
                        Extra.class, "E"
                )
                .body(
                        "public class $T {",
                        "",
                        "   @$E String mExtraString;",
                        "",
                        "}"
                );

        assertFiles(input)
                .with(BladeProcessor.Module.EXTRA)
                .failsToCompile()
                .withErrorContaining(ExtraErrorMsg.Invalid_class_with_Extra);
    }

    @Test
    public void invalidField() {
        JavaFileObject input = file("com.example", "MainActivity")
                .imports(
                        Extra.class, "E",
                        Activity.class
                )
                .body(
                        "public class $T extends Activity {",
                        "",
                        "   @$E private String mExtraString;",
                        "",
                        "}"
                );

        assertFiles(input)
                .with(BladeProcessor.Module.EXTRA)
                .failsToCompile()
                .withErrorContaining(String.format(ErrorMsg.Invalid_field_with_annotation, Extra.class.getSimpleName()));

        input = file("com.example", "MainActivity")
                .imports(
                        Extra.class, "E",
                        Activity.class
                )
                .body(
                        "public class $T extends Activity {",
                        "",
                        "   @$E protected String mExtraString;",
                        "",
                        "}"
                );

        assertFiles(input)
                .with(BladeProcessor.Module.EXTRA)
                .failsToCompile()
                .withErrorContaining(String.format(ErrorMsg.Invalid_field_with_annotation, Extra.class.getSimpleName()));

        input = file("com.example", "MainActivity")
                .imports(
                        Extra.class, "E",
                        Activity.class
                )
                .body(
                        "public class $T extends Activity {",
                        "",
                        "   @$E final String mExtraString;",
                        "",
                        "}"
                );

        assertFiles(input)
                .with(BladeProcessor.Module.EXTRA)
                .failsToCompile()
                .withErrorContaining(String.format(ErrorMsg.Invalid_field_with_annotation, Extra.class.getSimpleName()));
    }

    @Test
    public void activityNone() {
        JavaFileObject input = file("com.example", "MainActivity")
                .imports(
                        Blade.class, "B",
                        Activity.class
                )
                .body(
                        "@$B",
                        "public class $T extends Activity {}"
                );

        assertFiles(input)
                .with(BladeProcessor.Module.EXTRA)
                .compilesWithoutError()
                .and()
                .generatesFileNamed(StandardLocation.CLASS_OUTPUT, "blade", "I.class");
    }

    @Test
    public void activityTwoDefault() {
        JavaFileObject input = file("com.example", "MainActivity")
                .imports(
                        Extra.class, "E",
                        Activity.class
                )
                .body(
                        "public class $T extends Activity {",
                        "",
                        "   @$E String mExtraString;",
                        "   @$E int mA;",
                        "",
                        "}"
                );

        JavaFileObject expected = generatedFile("com.example", "MainActivity_Helper")
                .imports(
                        input, "I",
                        BundleWrapper.class,
                        Weave.class,
                        Intent.class
                )
                .body(
                        "abstract class $T {",
                        "",
                        "   @Weave(into = \"0^onCreate\", args = {\"android.os.Bundle\"}, statement = \"com.example.$T.inject(this);\")",
                        "   public static void inject($I target) {",
                        "       Intent intent = target.getIntent();",
                        "       if (intent == null || intent.getExtras() == null) {",
                        "           return;",
                        "       }",
                        "       BundleWrapper extras = BundleWrapper.from(intent.getExtras());",
                        "       target.mExtraString = extras.get(\"<Extra-mExtraString>\", target.mExtraString);",
                        "       target.mA = extras.get(\"<Extra-mA>\", target.mA);",
                        "   }",
                        "",
                        "}"
                );

        assertFiles(input)
                .with(BladeProcessor.Module.EXTRA)
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void activityOneCustomOneDefault() {
        final JavaFileObject customBundler = file("com.example", "StringBundler")
                .imports(
                        Bundler.class,
                        Bundle.class
                )
                .body(
                        "public class $T implements Bundler<String> {",
                        "",
                        "   public void save(String value, Bundle state) {",
                        "   }",
                        "",
                        "   public String restore(Bundle state) {",
                        "       return null;",
                        "   }",
                        "",
                        "}"
                );

        final JavaFileObject input = file("com.example", "MainActivity")
                .imports(
                        Extra.class, "E",
                        customBundler, "CB",
                        Activity.class
                )
                .body(
                        "public class $T extends Activity {",
                        "",
                        "   @$E($CB.class) String mExtraString;",
                        "   @$E int mA;",
                        "",
                        "}"
                );

        final JavaFileObject expected = generatedFile("com.example", "MainActivity_Helper")
                .imports(
                        input, "I",
                        customBundler, "CB",
                        BundleWrapper.class,
                        Weave.class,
                        Intent.class
                )
                .body(
                        "abstract class $T {",
                        "",
                        "   @Weave(into = \"0^onCreate\", args = {\"android.os.Bundle\"}, statement = \"com.example.$T.inject(this);\")",
                        "   public static void inject($I target) {",
                        "       Intent intent = target.getIntent();",
                        "       if (intent == null || intent.getExtras() == null) {",
                        "           return;",
                        "       }",
                        "       BundleWrapper extras = BundleWrapper.from(intent.getExtras());",
                        "       $CB mExtraStringBundler = new $CB();",
                        "       target.mExtraString = mExtraStringBundler.restore(extras.getBundle(\"<Extra-mExtraString>\"));",
                        "       target.mA = extras.get(\"<Extra-mA>\", target.mA);",
                        "   }",
                        "",
                        "}"
                );

        assertFiles(customBundler, input)
                .with(BladeProcessor.Module.EXTRA)
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void serviceTwoDefault() {
        JavaFileObject input = file("com.example", "SomeService")
                .imports(
                        Extra.class, "E",
                        Service.class,
                        Intent.class,
                        IBinder.class
                )
                .body(
                        "public class $T extends Service {",
                        "",
                        "   @$E String mExtraString;",
                        "   @$E int mA;",
                        "",
                        "   public IBinder onBind(Intent intent) {",
                        "       return null;",
                        "   }",
                        "",
                        "}"
                );

        JavaFileObject expected = generatedFile("com.example", "SomeService_Helper")
                .imports(
                        input, "I",
                        BundleWrapper.class,
                        Intent.class,
                        Weave.class
                )
                .body(
                        "abstract class $T {",
                        "",
                        "   @Weave(into = \"0^onStartCommand\", args = {\"android.content.Intent\", \"int\", \"int\"}, statement = \"com.example.$T.inject(this, $1);\")",
                        "   public static void inject($I target, Intent intent) {",
                        "       if (intent == null || intent.getExtras() == null) {",
                        "           return;",
                        "       }",
                        "       BundleWrapper extras = BundleWrapper.from(intent.getExtras());",
                        "       target.mExtraString = extras.get(\"<Extra-mExtraString>\", target.mExtraString);",
                        "       target.mA = extras.get(\"<Extra-mA>\", target.mA);",
                        "   }",
                        "",
                        "}"
                );

        assertFiles(input)
                .with(BladeProcessor.Module.EXTRA)
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void intentServiceTwoDefault() {
        JavaFileObject input = file("com.example", "SomeService")
                .imports(
                        Extra.class, "E",
                        IntentService.class,
                        Intent.class,
                        IBinder.class
                )
                .body(
                        "public class $T extends IntentService {",
                        "",
                        "   @$E String mExtraString;",
                        "   @$E int mA;",
                        "",
                        "   public $T() {super(\"Test\");}",
                        "",
                        "   @Override",
                        "   protected void onHandleIntent(Intent intent) {}",
                        "",
                        "}"
                );

        JavaFileObject expected = generatedFile("com.example", "SomeService_Helper")
                .imports(
                        input, "I",
                        BundleWrapper.class,
                        Intent.class,
                        Weave.class
                )
                .body(
                        "abstract class $T {",
                        "",
                        "   @Weave(into = \"0^onHandleIntent\", args = {\"android.content.Intent\"}, statement = \"com.example.$T.inject(this, $1);\")",
                        "   public static void inject($I target, Intent intent) {",
                        "       if (intent == null || intent.getExtras() == null) {",
                        "           return;",
                        "       }",
                        "       BundleWrapper extras = BundleWrapper.from(intent.getExtras());",
                        "       target.mExtraString = extras.get(\"<Extra-mExtraString>\", target.mExtraString);",
                        "       target.mA = extras.get(\"<Extra-mA>\", target.mA);",
                        "   }",
                        "",
                        "}"
                );

        assertFiles(input)
                .with(BladeProcessor.Module.EXTRA)
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }
}

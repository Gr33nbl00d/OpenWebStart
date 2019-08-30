package com.openwebstart.jvm;

import com.openwebstart.jvm.runtimes.LocalJavaRuntime;
import com.openwebstart.jvm.runtimes.Vendor;
import net.adoptopenjdk.icedteaweb.io.FileUtils;
import net.adoptopenjdk.icedteaweb.io.IOUtils;
import net.adoptopenjdk.icedteaweb.jnlp.version.VersionId;
import net.adoptopenjdk.icedteaweb.jnlp.version.VersionString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static com.openwebstart.jvm.os.OperationSystem.ARM32;
import static com.openwebstart.jvm.os.OperationSystem.MAC64;
import static com.openwebstart.jvm.runtimes.Vendor.ADOPT;
import static com.openwebstart.jvm.runtimes.Vendor.ANY_VENDOR;
import static com.openwebstart.jvm.runtimes.Vendor.ORACLE;

public class LocalRuntimeManagerTest {

    private static final VersionId VERSION_1_8_219 = VersionId.fromString("1.8.219");
    private static final VersionId VERSION_1_8_220 = VersionId.fromString("1.8.220");
    private static final VersionId VERSION_11_0_1 = VersionId.fromString("11.0.1");

    @BeforeEach
    public void init() throws Exception {
        final URL cacheFolderUrl = LocalRuntimeManagerTest.class.getResource("test-cache");
        final Path cacheFolder = Paths.get(cacheFolderUrl.toURI());

        final File cacheConfigTemplateFile = new File(cacheFolder.toFile(), "cache.template.json");
        final File cacheConfigFile = new File(cacheFolder.toFile(), "cache.json");

        if(cacheConfigFile.exists()) {
            cacheConfigFile.delete();
        }

        final FileInputStream templateInputStream = new FileInputStream(cacheConfigTemplateFile);
        final String content = IOUtils.readContentAsUtf8String(templateInputStream);
        final String cacheConfig = content.replace("{CACHE_FOLDER}", cacheFolder.toUri().toString());
        FileUtils.saveFileUtf8(cacheConfig, cacheConfigFile);


        RuntimeManagerConfig.setDefaultVendor(null);
        RuntimeManagerConfig.setSupportedVersionRange(null);


        RuntimeManagerConfig.setCachePath(cacheFolder);
        LocalRuntimeManager.getInstance().loadRuntimes();
    }

    @AfterEach
    public void reset() {
        RuntimeManagerConfig.setDefaultVendor(null);
        RuntimeManagerConfig.setSupportedVersionRange(null);
    }

    @Test
    public void checkBestRuntime_1() {
        //given
        final VersionString versionString = VersionString.fromString("1.8*");

        //when
        final LocalJavaRuntime runtime = LocalRuntimeManager.getInstance().getBestRuntime(versionString, ANY_VENDOR, MAC64);

        //than
        Assertions.assertNotNull(runtime);
        Assertions.assertEquals(VERSION_1_8_220, runtime.getVersion());
        Assertions.assertEquals(ADOPT, runtime.getVendor());
        Assertions.assertEquals(MAC64, runtime.getOperationSystem());
        Assertions.assertTrue(runtime.isManaged());
        Assertions.assertTrue(runtime.isActive());
    }

    @Test
    public void checkBestRuntime_2() {
        //given
        final VersionString versionString = VersionString.fromString("1.8+");

        //when
        final LocalJavaRuntime runtime = LocalRuntimeManager.getInstance().getBestRuntime(versionString, ANY_VENDOR, MAC64);

        //than
        Assertions.assertNotNull(runtime);
        Assertions.assertEquals(VERSION_11_0_1, runtime.getVersion());
        Assertions.assertEquals(ADOPT, runtime.getVendor());
        Assertions.assertEquals(MAC64, runtime.getOperationSystem());
        Assertions.assertTrue(runtime.isManaged());
        Assertions.assertTrue(runtime.isActive());
    }

    @Test
    public void checkBestRuntime_3() {
        //given
        final VersionString versionString = VersionString.fromString("1.8*");

        //when
        LocalRuntimeManager.getInstance().getAll().stream()
                .filter(r -> Objects.equals(r.getVersion(), VERSION_1_8_220))
                .forEach(r -> {
                    final LocalJavaRuntime modified = r.getDeactivatedCopy();
                    LocalRuntimeManager.getInstance().replace(r, modified);
                });
        final LocalJavaRuntime runtime = LocalRuntimeManager.getInstance().getBestRuntime(versionString, ANY_VENDOR, MAC64);

        //than
        Assertions.assertNotNull(runtime);
        Assertions.assertEquals(VERSION_1_8_219, runtime.getVersion());
        Assertions.assertEquals(ORACLE, runtime.getVendor());
        Assertions.assertEquals(MAC64, runtime.getOperationSystem());
        Assertions.assertTrue(runtime.isManaged());
        Assertions.assertTrue(runtime.isActive());
    }

    @Test
    public void checkBestRuntime_4() {
        //given
        VersionString versionString = VersionString.fromString("1.8*");

        //when
        LocalJavaRuntime runtime = LocalRuntimeManager.getInstance().getBestRuntime(versionString, ORACLE, MAC64);

        //than
        Assertions.assertNotNull(runtime);
        Assertions.assertEquals(VERSION_1_8_219, runtime.getVersion());
        Assertions.assertEquals(ORACLE, runtime.getVendor());
        Assertions.assertEquals(MAC64, runtime.getOperationSystem());
        Assertions.assertTrue(runtime.isManaged());
        Assertions.assertTrue(runtime.isActive());
    }

    @Test
    public void checkBestRuntime_5() {
        //given
        final VersionString versionString = VersionString.fromString("1.8+");

        //when
        RuntimeManagerConfig.setSupportedVersionRange(VersionString.fromString("1.8*"));
        final LocalJavaRuntime runtime = LocalRuntimeManager.getInstance().getBestRuntime(versionString, ANY_VENDOR, MAC64);

        //than
        Assertions.assertNotNull(runtime);
        Assertions.assertEquals(VERSION_1_8_220, runtime.getVersion());
        Assertions.assertEquals(ADOPT, runtime.getVendor());
        Assertions.assertEquals(MAC64, runtime.getOperationSystem());
        Assertions.assertTrue(runtime.isManaged());
        Assertions.assertTrue(runtime.isActive());
    }

    @Test
    public void checkBestRuntime_8() {
        //given
        VersionString versionString = VersionString.fromString("1.8*");
        Vendor vendor = Vendor.fromString("not_found");

        //when
        LocalJavaRuntime runtime = LocalRuntimeManager.getInstance().getBestRuntime(versionString, vendor, MAC64);

        //than
        Assertions.assertNull(runtime);
    }

    @Test
    public void checkBestRuntime_9() {
        //given
        VersionString versionString = VersionString.fromString("1.8*");

        //when
        LocalJavaRuntime runtime = LocalRuntimeManager.getInstance().getBestRuntime(versionString, ANY_VENDOR, ARM32);

        //than
        Assertions.assertNull(runtime);
    }

    @Test
    public void checkBestRuntime_10() {
        //given
        VersionString versionString = VersionString.fromString("20*");

        //when
        LocalJavaRuntime runtime = LocalRuntimeManager.getInstance().getBestRuntime(versionString, ANY_VENDOR, MAC64);

        //than
        Assertions.assertNull(runtime);
    }
}
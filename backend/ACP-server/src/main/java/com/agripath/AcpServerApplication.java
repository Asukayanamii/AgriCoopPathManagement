package com.agripath;

import com.agripath.acpcommon.utils.NativeLibLoader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@Slf4j
public class AcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AcpServerApplication.class, args);
    }

    @PostConstruct
    public void initNativeLibs() {
        try {
            NativeLibLoader.loadLibraries();
            log.info("Native algorithm libraries loaded successfully");
        } catch (Exception e) {
            log.warn("Native library loading failed (algorithm requests may fall back): {}", e.getMessage());
        }
    }
}

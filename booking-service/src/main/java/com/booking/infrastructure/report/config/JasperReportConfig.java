package com.booking.infrastructure.report.config;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class JasperReportConfig {

    private static final Logger log = LoggerFactory.getLogger(JasperReportConfig.class);

    @Value("${app.report.template-path:classpath:jasper/templates/}")
    private String templatePath;

    @Value("${app.report.default-locale:vi_VN}")
    private String defaultLocaleStr;

    @Bean
    public Locale reportDefaultLocale() {
        String[] parts = defaultLocaleStr.split("_");
        return parts.length == 2
                ? new Locale(parts[0], parts[1])
                : new Locale(parts[0]);
    }

    @Bean
    public Map<String, JasperReport> jasperReportCache() {
        Map<String, JasperReport> cache = new ConcurrentHashMap<>();

        var resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(templatePath + "*.jrxml");
            if (resources.length == 0) {
                log.warn("No .jrxml templates found at {}", templatePath);
                return cache;
            }

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                String templateName = filename.replace(".jrxml", "");
                try (InputStream is = resource.getInputStream()) {
                    JasperReport report = JasperCompileManager.compileReport(is);
                    cache.put(templateName, report);
                    log.info("Compiled: {} -> cached as '{}'", filename, templateName);
                }
            }

            log.info("JasperReports: {} template(s) compiled", cache.size());

        } catch (IOException | JRException e) {
            log.error("JasperReports compile failed - some reports may not work");
            return cache;
        }

        return cache;
    }
}
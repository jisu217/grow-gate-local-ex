package com.growgate.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSeedCredentialsTest {

    @Test
    void adminSeedPasswordMatchesPresentationPassword() throws Exception {
        Path seedSql = Path.of(System.getProperty("user.dir"))
                .resolve("../../database/mysql/init/02_seed.sql")
                .normalize();
        String sql = Files.readString(seedSql);
        var matcher = Pattern.compile("'admin@growgate\\.com', '([^']+)'").matcher(sql);

        assertThat(matcher.find()).isTrue();
        assertThat(new BCryptPasswordEncoder().matches("growgateadmin", matcher.group(1))).isTrue();
        assertThat(sql).contains("'admin@growgate.com'", "'seed-demo-not-live'");
    }
}

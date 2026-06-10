package com.equipofutbol.equipofutbol_adso.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StaticFrontendTest {

    private static final Path STATIC_DIR = Path.of("src", "main", "resources", "static");

    @Test
    void frontendStaticFilesExist() {
        assertThat(STATIC_DIR.resolve("index.html")).exists().isRegularFile();
        assertThat(STATIC_DIR.resolve("styles.css")).exists().isRegularFile();
        assertThat(STATIC_DIR.resolve("app.js")).exists().isRegularFile();
    }

    @Test
    void indexContainsMainFunctionalForms() throws IOException {
        String index = Files.readString(STATIC_DIR.resolve("index.html"));

        assertThat(index)
                .contains("id=\"loginForm\"")
                .contains("id=\"registerForm\"")
                .contains("id=\"playerForm\"")
                .contains("id=\"trainingForm\"")
                .contains("id=\"loadTopButton\"")
                .contains("id=\"topPlayers\"");
    }

    @Test
    void javascriptTargetsBackendEndpoints() throws IOException {
        String app = Files.readString(STATIC_DIR.resolve("app.js"));

        assertThat(app)
                .contains("/auth/login")
                .contains("/auth/register")
                .contains("/user")
                .contains("/resultados")
                .contains("/user/top5")
                .contains("Authorization")
                .contains("localStorage");
    }
}

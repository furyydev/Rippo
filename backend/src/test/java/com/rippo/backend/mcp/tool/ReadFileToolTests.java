package com.rippo.backend.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rippo.backend.service.GitHubService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReadFileToolTests {

    private final GitHubService gitHubService = mock(GitHubService.class);
    private final ReadFileTool tool = new ReadFileTool(gitHubService);

    @Test
    void readsSupportedFileThroughGitHubService() {
        Map<String, Object> file = Map.of(
                "path", "src/App.java",
                "size", 42,
                "content", "class App {}"
        );
        when(gitHubService.getFileContent("owner", "repo", "src/App.java", "token"))
                .thenReturn(file);
        Map<String, Object> arguments = arguments("src/App.java");

        tool.validate(arguments);
        Object result = tool.execute(arguments, new ToolExecutionContext("token"));

        assertThat(result).isEqualTo(file);
        verify(gitHubService).getFileContent("owner", "repo", "src/App.java", "token");
    }

    @Test
    void rejectsUnsupportedFileExtensionWithStructuredCode() {
        assertThatThrownBy(() -> tool.validate(arguments("image.png")))
                .isInstanceOfSatisfying(ToolExecutionException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("UNSUPPORTED_FILE_TYPE")
                );
    }

    @Test
    void supportsAllRequestedTextFileFamilies() {
        for (String path : new String[]{
                "A.java", "A.kt", "pom.xml", "app.yaml", "data.json", "README.md",
                "build.gradle", "app.properties", "main.dart", "app.ts", "app.js", "notes.txt"
        }) {
            tool.validate(arguments(path));
        }
    }

    private Map<String, Object> arguments(String path) {
        return Map.of(
                "repositoryOwner", "owner",
                "repositoryName", "repo",
                "filePath", path
        );
    }
}

package dev.springreview.parser;

import com.github.javaparser.ast.CompilationUnit;

import java.util.List;

/**
 * M09 输出：单文件解析结果。
 * compilationUnit 供 AST 精细检测器使用；不进入 JSON 报告。
 */
public record ParsedSource(
    String path,
    String sourceHash,
    String packageName,
    List<ImportInfo> imports,
    List<TypeInfo> types,
    List<MethodInfo> methods,
    DependencyGraph dependencyGraph,
    List<ParseError> parseErrors,
    CompilationUnit compilationUnit
) {

    public record ImportInfo(
        String name,
        boolean isStatic,
        boolean isWildcard,
        int line
    ) {
    }

    public record TypeInfo(
        String qualifiedName,
        String simpleName,
        String kind,
        List<String> modifiers,
        List<String> annotations,
        String extendsType,
        List<String> implementsTypes,
        int beginLine,
        int endLine
    ) {
    }

    public record MethodInfo(
        String ownerType,
        String name,
        List<String> paramTypes,
        String returnType,
        List<String> modifiers,
        List<String> annotations,
        int beginLine,
        int endLine
    ) {
    }

    public boolean isParsed() {
        return parseErrors.stream().noneMatch(e ->
            e.errorType() != ParseError.ErrorType.SKIP);
    }
}

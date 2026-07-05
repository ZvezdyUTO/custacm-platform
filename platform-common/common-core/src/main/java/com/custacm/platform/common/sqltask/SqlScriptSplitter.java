package com.custacm.platform.common.sqltask;

import java.util.ArrayList;
import java.util.List;

final class SqlScriptSplitter {
    private SqlScriptSplitter() {
    }

    static List<String> split(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int index = 0; index < script.length(); index++) {
            char currentChar = script.charAt(index);
            char nextChar = index + 1 < script.length() ? script.charAt(index + 1) : '\0';

            if (inLineComment) {
                current.append(currentChar);
                if (currentChar == '\n' || currentChar == '\r') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                current.append(currentChar);
                if (currentChar == '*' && nextChar == '/') {
                    current.append(nextChar);
                    index++;
                    inBlockComment = false;
                }
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && currentChar == '-' && nextChar == '-') {
                current.append(currentChar).append(nextChar);
                index++;
                inLineComment = true;
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && currentChar == '#') {
                current.append(currentChar);
                inLineComment = true;
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && currentChar == '/' && nextChar == '*') {
                current.append(currentChar).append(nextChar);
                index++;
                inBlockComment = true;
                continue;
            }
            if (!inDoubleQuote && currentChar == '\'') {
                current.append(currentChar);
                if (inSingleQuote && nextChar == '\'') {
                    current.append(nextChar);
                    index++;
                } else {
                    inSingleQuote = !inSingleQuote;
                }
                continue;
            }
            if (!inSingleQuote && currentChar == '"') {
                current.append(currentChar);
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && currentChar == ';') {
                addStatement(statements, current);
                current.setLength(0);
                continue;
            }
            current.append(currentChar);
        }
        addStatement(statements, current);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isBlank()) {
            statements.add(statement);
        }
    }
}

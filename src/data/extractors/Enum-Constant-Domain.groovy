/*
 * Script for generate a oracle constant enum from a cg_ref_codes table
 * Copyright (c) 2020 by Maicon Rafael Zucco
 * https://github.com/maiconzucco/intellij_groovy_scripts/
 */

import com.intellij.database.util.Case
import java.text.Normalizer
import java.util.regex.Pattern

NEWLINE = System.getProperty("line.separator")
TAB = "  ";

def pascalCase(str) {
    com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
}

def String deAccent(String str) {
    String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    return pattern.matcher(nfdNormalizedString).replaceAll("");
}

def String formatConstantName(String rvMeaning) {
    rvMeaning = rvMeaning.replace(" ", "_");
    return  "cv" + pascalCase(deAccent(rvMeaning));
}

def record(columns, dataRow) {
    def lineConstant = "";
    def colName = dataRow.value(columns[0]);
    def colValue = dataRow.value(columns[4]);
    def constName =  formatConstantName(colName + "_" + colValue);

    lineConstant = TAB + constName + " CONSTANT minfin.cg_ref_codes.rv_low_value%TYPE := '"  +  dataRow.value(columns[1]) + "';" + NEWLINE;

    OUT.append(lineConstant);
}

ROWS.each { row -> record(COLUMNS, row) }

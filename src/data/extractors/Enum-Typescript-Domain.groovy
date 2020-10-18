import com.intellij.database.util.Case

import java.text.Normalizer
import java.util.regex.Pattern

NEWLINE   = System.getProperty("line.separator")
TAB       = "  ";

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

def String getEnumName(String colName) {
    return "Tipo" + pascalCase(colName.substring(2));
}

def String getConstantName(String rvMeaning) {
    def constantName = rvMeaning.replace("-", "_");
    constantName = constantName.replace(" ", "_");
    constantName = deAccent(constantName);
    return constantName.toUpperCase();
}

def record(columns, dataRow) {
    if (dataRow.first()) {
        def colName = dataRow.value(columns[0]);
        def enumName = getEnumName(colName);
        OUT.append("export enum ").append(enumName).append(" {").append(NEWLINE);
    }

    def colName = dataRow.value(columns[0]);
    def constantName = getConstantName(dataRow.value(columns[4]));
    def constantValue = dataRow.value(columns[1]);
    OUT.append(TAB).append(constantName).append(" = '").append(constantValue).append("'");

    if (dataRow.last()) {
        OUT.append(NEWLINE);
    } else {
        OUT.append(",").append(NEWLINE);
    }
}

ROWS.each { row -> record(COLUMNS, row) }

OUT.append("}").append(NEWLINE);

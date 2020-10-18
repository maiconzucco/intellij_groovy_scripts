/*
 * Copyright (c) 2020 by Maicon Rafael Zucco
 *  https://github.com/maiconzucco/intellij_groovy_scripts/
 */
import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

NEWLINE = System.getProperty("line.separator")
TAB = "  ";

typeMapping = [
        (~/(?i)int/)                      : "long",
        (~/(?i)float|double|decimal|real/): "double",
        (~/(?i)datetime|timestamp/)       : "java.sql.Timestamp",
        (~/(?i)date/)                     : "java.sql.Date",
        (~/(?i)time/)                     : "java.sql.Time",
        (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

tableOner = null;
apiOwner = null;
tableName = null;
tableShortName = null;
fields = null;
tpRecName = null;
tpTabName = null;

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           name : javaName(col.getName(), false),
                           colName : col.getName(),
                           type : typeStr,
                           annos: ""]]
    }
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
    .collect { Case.LOWER.apply(it).capitalize() }
    .join("")
    .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

def pascalCase(str) {
    com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
}

def getApiName(String tableName) {
    return "pkg_api_" + tableName.substring(3).toLowerCase();
}

def generate(table, dir) {
    tableOwner = DasUtil.getSchema(table);
    apiOwner = tableOwner.toUpperCase() + "_APP";
    tableName = table.getName();
    tableShortName = tableName.substring(3);
    apiName = getApiName(tableName);
    fields = calcFields(table)

    new File(dir, apiName + ".pks").withPrintWriter { out -> generatePks(out) }
    new File(dir, apiName + ".pkb").withPrintWriter { out -> generatePkb(out) }
}

def generateType(out) {
    tpRecName = "rec_" + tableShortName;
    tpTabName = "tab_" + tableShortName;
    out.println("TYPE $tpRecName IS RECORD(")

    fields.each() {
        if (it != fields.first()) {
            out.print ","
        }
        out.println "${it.colName} $tableOwner.$tableName.${it.colName}%TYPE";
      }
    out.println(",total_linhas    NUMBER");
    out.println(",nro_linha       NUMBER");

    out.println ""
    out.println "TYPE $tpTabName IS TABLE OF $tpRecName INDEX BY PLS_INTEGER;"
}

def generatePks(out) {
    out.println "CREATE OR REPLACE PACKAGE $apiOwner.$apiName IS"
    generateType(out)
    out.println ""
    out.println "END $apiName;"
    out.println "/"
}

def generatePkb(out) {
    out.println "CREATE OR REPLACE PACKAGE BODY $apiOwner.$apiName IS"
    out.println ""
    out.println "END $apiName;"
    out.println "/"
}

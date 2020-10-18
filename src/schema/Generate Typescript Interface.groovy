import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Script for generate a interface typescript from a table
 * Original script https://github.com/yasikovsky/intellij-poco-scripts/blob/master/Typescript%20POCO%20to%20clipboard.groovy
 */
typeMapping = [
        (~/(?i)^bit$|tinyint\(1\)/)                      : "boolean",
        (~/(?i)^tinyint$/)                               : "number",
        (~/(?i)^uniqueidentifier|uuid$/)                 : "string",
        (~/(?i)^int|integer$/)                           : "number",
        (~/(?i)^bigint$/)                                : "number",
        (~/(?i)^varbinary|image$/)                       : "any[]",
        (~/(?i)^double|float|real$/)                     : "number",
        (~/(?i)^decimal|money|numeric|smallmoney$/)      : "number",
        (~/(?i)^datetimeoffset$/)                        : "Date",
        (~/(?i)^datetime|datetime2|timestamp|date|time$/): "Date",
        (~/(?i)^char$/)                                  : "string",

        (~/(?i)^VARCHAR2$/)                              : "string",
        (~/(?i)^CLOB$/)                                  : "string",
        (~/(?i)^BLOB$/)                                  : "byte[]",
        (~/(?i)^NUMBER$/)                                : "number",
        (~/(?i)^DATE$/)                                  : "Date"
]

notNullableTypes = []
tempString = '';

SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it) }
CLIPBOARD.set(tempString)

def String toCamelCase( String text) {
    text = text.toLowerCase();
    text = text.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } );
    return text;
}

def String getType(String colName, String spec) {
    def type;

    if (colName.toLowerCase().startsWith("fl")) {
        type = "SimNao";
    } else {
        if (colName.toLowerCase().startsWith("tp")) {
            type = "Tipo" + toCamelCase(colName).substring(2);
        } else {
            type = typeMapping.find { p, t -> p.matcher(spec).find() }?.value ?: "any";
        }
    }

    return type;
}

def generate(table) {
    def className = pascalCase(table.getName().substring(2))
    def fields = calcFields(table)

    StringWriter out = new StringWriter();

    if (tempString != '') {
        out.println ""
    }

    generate(out, className, fields, table);

    tempString += out.toString();
}

def generate(out, className, fields, table) {
    out.println "export default interface $className {"

    fields.each() {
        if (it.comment != "") {
            out.println "";
            out.println "    //${it.comment}";
        }

        def line = "    ${it.name}: ${it.type}"

        if (it != fields.last()) {
            line += ","
        }

        if (it.comment != "") {
            line += " // ${it.comment}"
        }

        out.println "${line}"
    }
    out.println "}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification()).replaceAll("\\(.*?\\)","");
        def typeStr = getType(col.getName(), spec)
        def nullable = col.isNotNull() || typeStr in notNullableTypes ? "" : "?"
        def pk = DasUtil.getPrimaryKey(table).toString();
        def name = toCamelCase(col.getName());

        fields += [[
                           primarykey: pk != null && pk != "" && pk.contains("(${col.getName()})") ? true : false,
                           colname   : col.getName(),
                           spec      : spec,
                           name      : name + nullable,
                           type      : typeStr,
                           comment   : col.comment ? col.comment : ""]]
    }
}

def camelCase(str) {
    def dict = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str).collect()
    def result = '';

    dict.forEach { value ->
        if (result == '')
            result += value;
        else result += value.capitalize()
    }

    return result;
}

def pascalCase(str) {
    com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
}

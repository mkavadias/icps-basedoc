package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.net.ssl.HttpsURLConnection;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.json.JSONArray;
import org.json.JSONObject;

// import com.google.common.base.Preconditions;

//
// utilities 
//
public class CbsUtils {

  // converts MS AD long to java TS
  public static Timestamp msToTimestamp(long l) {
    final long adAdjust = 11644473600000L;  // adjust factor
    return new Timestamp(l / 10000 - adAdjust);
  }

  //
  // ==================================================================================

  /**
   * Extract the SSO id from an account , given its name
   * 
   * @param account
   * @return
   */
  public static String getSsoid(String account) {
    checkNotNull(account, "Null argument");
    String low = account.toLowerCase();
    String ssoid = "";
    if (low.startsWith("f8"))
      ssoid = account.substring(2, 5).toUpperCase();
    else if (low.startsWith("f9"))
      ssoid = account.substring(2, 5).toUpperCase();
    else if (low.startsWith("myint"))
      ssoid = "WPS";
    else if (low.startsWith("intranet"))
      ssoid = "INT";
    else if (low.startsWith("krb"))
      ssoid = "TGT";
    else if (low.startsWith("kerberos"))
      ssoid = "SAP";
    else if (low.endsWith("$"))
      ssoid = low.substring(0, 3).toUpperCase();
    else if (low.startsWith("mssql"))
      ssoid = "SQL";
    else if (account.startsWith("egate"))
      ssoid = "OEG";
    else if (account.startsWith("earlyocr"))
      ssoid = "OCR";
    else if (account.startsWith("eocr"))
      ssoid = "OCR";
    else if (account.startsWith("tda"))
      ssoid = "TDA";
    else {
      System.out.println(account + " fail to find SSOID");
    }
    return ssoid;
  }

  /**
   * Extract the SSO id from an account , given its name
   * 
   * @param account
   * @return
   */
  public static String guessEnv(String account) {
   checkNotNull(account, "Null argument");
    String env = "PROD";
    if (account.startsWith("F")) {
      char c = account.charAt(5);
      switch (c) {
      case 'T':
      case 'D':
        env = "TEST";
        break;
      case 'I':
        env = account.endsWith("0") ? "I0" : "INTG";
        break;
      case 'U':
        env = "UAT";
        break;
      case 'O':
        env = "OSA";
        break;
      case 'M': // F9MNGM3 ??
        env = "OSA";
        break;
      case 'P':
        env = "PROD";
        break;
      default:
        System.err.println("F " + account + " issue;");
        break;
      }
    } else if (account.endsWith("-T")) {
      env = "TEST";
    } else if (account.endsWith("-I")) {
      env = "INTG";
    } else if (account.endsWith("-I0")) {
      env = "I0";
    } else if (account.endsWith("-U")) {
      env = "UAT";
    } else if (account.endsWith("-O")) {
      env = "OSA";
    } else if (account.endsWith("-P")) {
      env = "PROD";
    } else if (account.startsWith("MSSQL")) {
      if (account.endsWith("10"))
        env = "TEST";
      else if (account.endsWith("11"))
        env = "TEST";
      else if (account.endsWith("12"))
        env = "TEST";
      else if (account.endsWith("21"))
        env = "INTG";
      else if (account.endsWith("22"))
        env = "UAT";
      else if (account.endsWith("23"))
        env = "OSA";
    } else if (account.startsWith("KERBEROSD")) { // SAP
      env = "TEST";
    } else if (account.startsWith("KERBEROSI")) { // SAP
      env = "INTG";
    } else {
      System.err.println(account + " " + env);
    }
    return env;
  }
  //
  // ==================================================================================

  // reads fully file
  public static String readFile(String fn) {
    try {
      //
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fn), "UTF8"));
      // BufferedReader in = new BufferedReader(new FileReader(fn));
      String result = "";
      String inputLine;
      while ((inputLine = in.readLine()) != null)
        result += inputLine + " ";
      in.close();

      return result;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  // reads fully file
  public static String readFully(File file) {
    try {
      //
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
      // BufferedReader in = new BufferedReader(new FileReader(fn));
      String result = "";
      String inputLine;
      while ((inputLine = in.readLine()) != null)
        result += inputLine + " ";
      in.close();

      return result;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  // reads fully file returns a list of lines
  public static List<String> file2list(File file) {
    try {
      // only utf-8
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
      //
      // BufferedReader in = new BufferedReader(new FileReader(file));
      List<String> result = new ArrayList<>();
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        result.add(inputLine);
      }
      in.close();

      return result;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  // read a csv file and returns a json array
  public static JSONArray readCsvFile(File file) {
    final String comma = ",";
    List<String> list = file2list(file);
    String hdrline = list.remove(0);
    StringTokenizer tokenizer = new StringTokenizer(hdrline, comma);
    int hdrlen = tokenizer.countTokens();
    String[] headers = new String[hdrlen];
    String token = null;
    int index = 0;
    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken();
      headers[index] = token;
      index++;
    }
    JSONArray rc = new JSONArray();
    JSONObject cur = null;
    for (String s : list) {
      cur = new JSONObject();
      tokenizer = new StringTokenizer(s, comma);
      for (index = 0; index < hdrlen; index++) {
        if (tokenizer.hasMoreTokens()) {
          cur.put(headers[index], tokenizer.nextToken());
        }
      }
      rc.put(cur);
    }
    return rc;
  }

  //
  // ============================================================================
  //

  //
  // Reads url, returns null if problem occurs
  // returns line delimitted
  //
  public static String readUrl(String url) {
    try {
      URLConnection uc = (new URL(url)).openConnection();
      // TODO basic auth adding uc.setRequestProperty("Authorization", basicAuth);
      HttpsURLConnection httpConn = (HttpsURLConnection) uc;
      //
      BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
      String result = "";
      String inputLine;
      while ((inputLine = in.readLine()) != null)
        result += inputLine + "\n";
      in.close();

      return result;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  //
  // Reads url, returns null if problem occurs
  // returns list line based
  //
  public static List<String> url2list(String url) {
    try {
      URLConnection uc = (new URL(url)).openConnection();
      // TODO basic auth adding uc.setRequestProperty("Authorization", basicAuth);
      HttpURLConnection httpConn = (HttpURLConnection) uc;
      //
      BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
      List<String> result = new ArrayList<>();
      String inputLine;
      while ((inputLine = in.readLine()) != null)
        result.add(inputLine);
      in.close();

      return result;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  //
  // ============================================================================
  //

  public static void printMap(Map<String, ?> map, PrintStream file) {
    for (String key : map.keySet()) {
      Object obj = map.get(key);
      if (obj == null) {
        System.out.println("Prop list null for " + key);
        continue;
      }
      file.printf("%-52s %s\n", key, obj.toString());
    }
  }

  public static void printMap(Map<String, ?> map, String filename) {
    PrintStream file = null;
    try {
      file = new PrintStream(filename, "utf8");
      printMap(map, file);
    } catch (IOException e) {
      e.printStackTrace();
      file = null;
    }
    if (file != null) file.close();
  }

  public static void printValues(Map<String, ?> map, File afile) {
    PrintStream file = null;
    try {
      file = new PrintStream(afile, "utf8");
      for (Object obj : map.values()) {
        file.println(obj.toString());
      }
    } catch (IOException e) {
      e.printStackTrace();
      file = null;
    }
    if (file != null) file.close();
  }

  // ------------------------------------------------------------------------------------
  /**
   * Creates a JSOn object out of an xls sheet. Assumes first row contains header info
   * 
   * @param File
   *          file containing the sheet
   */

  public static JSONArray readXlsSheet(File file, int sheet) throws InvalidFormatException, IOException {
    Sheet xlsSheet = WorkbookFactory.create(file).getSheetAt(sheet);
    return readXlsSheet(xlsSheet);
  }

  /**
   * Creates a JSOn object out of an xls sheet. Assumes first row contains header info
   * 
   * @param File
   *          file containing the sheet
   */

  public static JSONArray readXlsSheet(File file) throws InvalidFormatException, IOException {
    Sheet xlsSheet = WorkbookFactory.create(file).getSheetAt(0);
    return readXlsSheet(xlsSheet);
  }

  /** Creates a JSON object out of an xls sheet */
  private static JSONArray readXlsSheet(Sheet xlsSheet) {

    int last = 0, rf = xlsSheet.getFirstRowNum(), rl = xlsSheet.getLastRowNum();
    JSONArray rc = new JSONArray();
    // assume Headers are in first row
    Row names = xlsSheet.getRow(rf);
    Cell cell = null;
    int f = names.getFirstCellNum(), l = names.getLastCellNum();
    String atts[] = new String[l];
    // System.out.printf("Headers : %d - %d \n", f, l);
    for (int i = f; i < l; i++) {
      cell = names.getCell(i);
      if (cell == null) {
        atts[i] = "NN-" + (last++);
      } else {
        atts[i] = cell.getStringCellValue();
      }
    }

    Row row = null;
    JSONObject dat = null;
    for (int tr = 1; tr < rl; tr++) {
      row = xlsSheet.getRow(tr);
      if (row != null) {
        dat = fetchRow(row, atts);
        rc.put(dat);
      }
    }
    return rc;

  }

  private static JSONObject fetchRow(Row row, String[] names) {
    final SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd");
    JSONObject rc = new JSONObject();
    int f = row.getFirstCellNum(), l = row.getLastCellNum();
    if (l > names.length) l = names.length;
    Cell cell = null;
    String val = null;
    // String _null = null;
    for (int i = f; i < l; i++) {
      cell = row.getCell(i);
      if (cell != null) {
        // System.out.println(cell.getCellType());
        switch (cell.getCellType()) {
        case Cell.CELL_TYPE_BLANK:
          val = "";
          break;
        case Cell.CELL_TYPE_BOOLEAN:
          val = "" + cell.getBooleanCellValue();
          break;
        case Cell.CELL_TYPE_ERROR:
          val = "_ERR_";
          break;
        case Cell.CELL_TYPE_FORMULA:
          val = "F" + cell.getCellFormula();
          // val = cell.get
          break;
        case Cell.CELL_TYPE_NUMERIC:
          if (names[i].contains("Completion")) {
            // date
            val = fmt.format(cell.getDateCellValue());
          } else {
            val = "" + cell.getNumericCellValue();
          }
          break;
        case Cell.CELL_TYPE_STRING:
          val = cell.getStringCellValue();
          break;

        default:
          val = "_DEF_";
          break;
        }
        if (!"".equals(val)) {
          rc.put(names[i], val);
        }
      }
    }
    return rc;
  }

  // ==============================================================================================
  public static void checkNotNull(Object o, String msg) {
    if (o == null) {
      throw new IllegalArgumentException(msg);
    }
  }

  public static void checkIsTrue(boolean b, String msg) {
    if (!b) {
      throw new IllegalArgumentException(msg);
    }
  }
}

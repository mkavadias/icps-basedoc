package utils;

// import static epo.cbs.common.persistence.gen.ReadModel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.json.JSONArray;
import org.json.JSONObject;

// import org.cbs.common.persistence.model.*;

//import epo.cbs.common.persistence.pm.Association;

/**
 * Generates 'graphviz' .dot files.
 */
public class GenDoc_Dot { // extends PF_V5_Utils {

    /** Sub-directory where files will be generated. */
    static final String GEN_DIR = "data";

    // -------------------------------------------------------------
    // DOT specific parameters.
    // -------------------------------------------------------------

    private final int COMM_LEN = 80;

    private final float GREY0 = 1.f;

    private final float GREY1 = 0.92f;

    private final float GREY3 = 0.8f;
    // -------------------------------------------------------------
    // DOT specific parameters.
    // -------------------------------------------------------------

    final Map<String, String> subgraphs = new HashMap<String, String>();
    final Map<String, String> catgraphs = new HashMap<String, String>();

    /**
     * Used for stand alone generation.
     * 
     * @param args
     *             ignored
     */
    public static void main(String args[]) {
        String xlsFile = "cps-batch-flow.xlsx";
        String proc = "BATCH";
        GenDoc_Dot prog = new GenDoc_Dot();
        try {
            prog.createDoc(xlsFile, proc);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);
    }

    static String str_owners = "";
    static String str_child = "";

    static String[] rank = new String[11];

    /**
     * @param modelname
     * @throws IOException
     * @throws InvalidFormatException 
     */
    public void createDoc(String xls, String proc) throws IOException, InvalidFormatException {

        // 1. read file and convert xls to json
        File file = new File("data", xls);
        JSONArray rawData = CbsUtils.readXlsSheet(file);

        // 2. complete structure,
        JSONArray struct = completeStructure(rawData);

        // 3. filter procedure
        JSONArray batch = filter(struct, proc);
        printToFile(getFileStem(proc, ".json"),batch.toString(2));

        // 4. Make dot
        String result = makeDot(batch, proc);
        printToFile(getFileStem(proc,".dot"), result);
        
        
        // 5. Run neatto
        runNeatTo(proc);


    }

    // ------------------------------------------------------------------------
    // -- Private Methods --
    // ------------------------------------------------------------------------
    private final static String H_NEXT = "next";
    private final static String H_DB = "db";
    private final static String H_RUN = "run";

    private final static String H_BATCH = "batch";
    private final static String H_DESC = "desc";
    private final static String H_COMMENT = "comment";

    private final static String[] TO_COPY = { H_BATCH, H_DESC, H_COMMENT };
    private final static String[] TO_PROC = { H_BATCH, H_DESC, H_COMMENT, H_DB, H_NEXT, H_RUN };

    // 2. complete
    private JSONArray completeStructure(JSONArray rawData) {
        int len = rawData.length();
        JSONArray rc = new JSONArray();
        for (int i = 0; i < len; i++) {
            JSONObject json = rawData.getJSONObject(i);
            JSONObject result = new JSONObject(json, TO_COPY);

            toArray(json, result, H_NEXT);
            toArray(json, result, H_DB);
            toArray(json, result, H_RUN);

//            System.out.println(json);
//            System.out.println(result);
            rc.put(result);
        }
        return rc;
    }

    // 3. filter
    private JSONArray filter(JSONArray struct, String proc) {

        int len = struct.length();
        JSONArray rc = new JSONArray();
        for (int i = 0; i < len; i++) {
            JSONObject json = struct.getJSONObject(i);

            JSONArray runs = json.optJSONArray(H_RUN);
            if (runs != null) {
                for (int j = 0; j < runs.length(); j++) {
                    String run = runs.getString(j);
                    if (proc.equalsIgnoreCase(run)) {
                        JSONObject result = new JSONObject(json, TO_PROC);
                        rc.put(result);
                    }
                }
            }
        }
        return rc;
    }

    private static JSONObject toArray(JSONObject json, JSONObject result, String key) {

        String value = json.optString(key);
        if (value != null) {
            StringTokenizer tokenizer = new StringTokenizer(value, " ");
            while (tokenizer.hasMoreTokens()) {
                result.append(key, tokenizer.nextToken());
            }
        }
        return result;
    }

    static String newline = "\n";
    static String T0 = "    ";
    static String T1 = T0 + T0;
    
    private String makeDot(JSONArray data, String proc) {
        String result = "";
        result += "digraph " + proc + " " + newline;
        result += "{" + newline;
        result += T0 + "size = \"30,40\";" + newline;
        result += T0 + "overlap = false;" + newline; // MK 2019 no overlap
        result += T0 + "sep = \"+20.0\";" + newline; // MK 2019 no overlap
        result += T0 + "node [shape = box];" + newline;
        String rc1 = "";
        
        int len = data.length();
        
        for (int i = 0; i < len ; i++) {            
            rc1 += makeDotForObj(data.getJSONObject(i), proc);
        }
        for (String sub : subgraphs.values()) {
            result += sub;
            result += T0 + "}";
        }
        result += rc1;
        result += "}";
        return result;
    }

    private String getDisplayName(JSONObject json) {

        String result = json.getString(H_BATCH).replace('>',' ').replaceAll("-", "_");
        
        
        return result.trim();
    }

    private String getDisplayName(String json) {
       
        String result = json.replace('>',' ').replaceAll("-", "_");
        
        return result.trim();
    }
    
    private String makeDotForObj(JSONObject json, String proc) {

        String result = newline;
        String name = getDisplayName(json);

        String ss = subgraphs.get(proc);
        if (ss == null) {
            ss = T0 + "subgraph cluster_" + proc + " {";
            ss += newline;
            ss += T1 + "label = \"" + proc + "\";";
            ss += newline;
        }

        for (String child : getChildlist(json) ) {
//            ss += T1 + name + " -> " + child + ";";
            ss += T1 + name + " -> " + getDisplayName(child) + ";";
            ss += newline;
        }
        subgraphs.put(proc, ss);
        return result;

    }

    private String[] getChildlist(JSONObject json) {
        JSONArray array = json.optJSONArray(H_NEXT);
        if (array == null ) {
            return new String[0];
        }
        int len = array.length();
        String[] rc = new String[len];
        for (int i = 0; i < len; i++) {
            String next = array.getString(i);
            rc[i] = next;
        }
        return rc;
    }

    /**
     * Prints the result to file. In gen_DIR
     */
    protected void printToFile(String fileName, String result) {
        try {
            File file = new File(GEN_DIR, fileName);
            System.out.println("Creating " + file.getName());
            PrintWriter writer = new PrintWriter(new FileOutputStream(file));
            writer.print(result);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Run neatto command from graphviz
    // MK Maart 2019
    private boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    private String getFileStem( String proc, String ext) {
        return  "cps-batch-flow-"+proc+ ext;
    }

    private String neato(String proc) {
        File file = new File(GEN_DIR, getFileStem(proc, ".dot"));
        File out = new File(GEN_DIR, getFileStem(proc, ".png"));
        System.out.println(file.getAbsolutePath());
        System.out.println(out.getAbsolutePath());
        return "/usr/local/bin/dot -Tpng " + file.getAbsolutePath() + " -o " + out.getAbsolutePath();
        // return "/usr/local/bin/neato -Tpng " + file.getAbsolutePath() + " -o " +
        // out.getAbsolutePath();

        // return "neato -Tpng " + gen_DIR+"/"+getFileStem(".dot") + "-o " +
        // gen_DIR+"/"+getFileStem(".png");
    }

    private String neatoWin(String proc) {
        File file = new File(GEN_DIR, getFileStem(proc, ".dot"));
        File out = new File(GEN_DIR, getFileStem(proc, ".png"));
        System.out.println(file.getAbsolutePath());
        System.out.println(out.getAbsolutePath());
//        return "\"c:\\Program Files (x86)\\Graphviz2.38\\bin\\sfdp.exe\" -Tpng " + file.getAbsolutePath() + " -o "
//                + out.getAbsolutePath();
        return "\"c:\\Program Files (x86)\\Graphviz2.38\\bin\\neato.exe\" -Tpng " + file.getAbsolutePath() + " -o " + out.getAbsolutePath();
        // return "neato -Tpng " + gen_DIR+"/"+getFileStem(".dot") + "-o " +
        // gen_DIR+"/"+getFileStem(".png");
    }

    private void runNeatTo(String proc) throws IOException {
        Process process;
        if (isWindows) {
            // TODO win
            process = Runtime.getRuntime().exec(neatoWin(proc));
        } else {
            process = Runtime.getRuntime().exec(neato(proc));
        }

        try {
            System.out.println(process.waitFor());
            int len;
            if ((len = process.getErrorStream().available()) > 0) {
                byte[] buf = new byte[len];
                process.getErrorStream().read(buf);
                System.err.println("Command error:\t\"" + new String(buf) + "\"");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

package utils;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Reads the excel sheet. (process,db,nr,desc,batch,next,comment)
 * Here next is an unordered array of batch.
 * 
 * @author michail
 *
 */

public class ReadWorkload {

    private final static String H_NEXT = "next";
    private final static String H_DB = "db";

    private final static String H_BATCH = "batch";
    private final static String H_DESC = "desc";
    private final static String H_COMMENT = "comment";

    private final static String[] TO_COPY = { H_BATCH, H_DESC, H_COMMENT };

    static final String fn = "data/cps-batch-flow.xlsx";

    public static void main(String[] args) {

        File flow = new File(fn);

        try {
            JSONArray flowData = CbsUtils.readXlsSheet(flow);
            int len = flowData.length();
            for (int i = 0; i < len; i++) {
                JSONObject json = flowData.getJSONObject(i);
                JSONObject result = new JSONObject(json, TO_COPY);

                toArray(json, result, H_NEXT);
                toArray(json, result, H_DB);

//                String next = json.getString(H_NEXT);
//                StringTokenizer tokenizer = new StringTokenizer(next, " ");
//                while (tokenizer.hasMoreTokens()) {
//                    result.append(H_NEXT, tokenizer.nextToken());
//                }

                System.out.println(json);
                System.out.println(result);
            }
        } catch (InvalidFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
}

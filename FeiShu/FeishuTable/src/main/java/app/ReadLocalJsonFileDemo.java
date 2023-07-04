package app;

import com.alibaba.fastjson.JSON;

import java.io.*;

public class ReadLocalJsonFileDemo {
    public static void main(String[] args) throws IOException {
        File file = new File("C:\\data\\test.json");
        readerMethod(file);

    }

    private static void readerMethod(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        Reader reader = new InputStreamReader(new FileInputStream(file), "Utf-8");
        int ch= 0;
        StringBuffer sb = new StringBuffer();
        while((ch = reader.read()) != -1) {
            sb.append((char) ch);
        }
        fileReader.close();
        reader.close();
        String jsonStr = sb.toString();
        System.out.println(JSON.parseObject(jsonStr));
    }
}

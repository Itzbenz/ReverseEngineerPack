package Standalone;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class DiscordWebhookStandalone {
    private static final ExecutorService staticExecutorService = Executors.newCachedThreadPool();
    public URL realUrl = null;
    public String url;
    public JSONObject json = new JSONObject();
    public ExecutorService executorService = staticExecutorService;
    public String username = "Null", avatar_url;
    public long flushInterval = 4000;
    protected PrintStream printStream = null;
    
    public DiscordWebhookStandalone(String url) throws MalformedURLException {
        this(new URL(url));
        
    }
    
    
    public DiscordWebhookStandalone(URL url) {
        realUrl = url;
        this.url = url.toExternalForm();
    }
    
    public static void main(String[] args) throws MalformedURLException, InterruptedException, ExecutionException {
        DiscordWebhookStandalone webhookStandalone = new DiscordWebhookStandalone(args[0]);
        webhookStandalone.username = "TEst";
        System.setOut(webhookStandalone.asPrintStream());
        System.setErr(webhookStandalone.asPrintStream());
        for (int i = 0; i < 10; i++) {
            System.out.println("Kys");
        }
        try {
            throw new NullPointerException("Test");
        }catch(NullPointerException e){
            e.printStackTrace();
        }
        Thread.sleep(4000);
        System.out.println("Kys");
    }
    
    public void post(String dat) throws IOException {
        
        // build connection
        HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
        // set request properties
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        // enable output and input
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        String doAnother = "";
        if (dat.length() > 1920){
            doAnother = dat.substring(1900);
            dat = dat.substring(0, 1900);
        }
        
        json.put("content", dat);
        json.put("username", username);
        json.put("avatar_url", avatar_url);
        conn.getOutputStream().write(json.toString().getBytes(StandardCharsets.UTF_8));
        conn.getOutputStream().flush();
        conn.getOutputStream().close();
        conn.getInputStream().close();
        conn.disconnect();
        if (!doAnother.isEmpty()){
            post(doAnother);
        }
        
    }
    
    public PrintStream asPrintStream() {
        long lastFlush = System.currentTimeMillis();
        final long[] nextFlush = {lastFlush + flushInterval};
        if (printStream == null) printStream = new PrintStream(new OutputStream() {
            StringBuilder sb = new StringBuilder();
            
            @Override
            public void write(int b) throws IOException {
                sb.append((char) b);
            }
            
            @Override
            public void flush() throws IOException {
                if (nextFlush[0] > System.currentTimeMillis()) return;
                executorService.submit(() -> {
                    try {
                        post(sb.toString());
                    }catch(IOException e){ }
                });
                sb = new StringBuilder();
                nextFlush[0] = System.currentTimeMillis() + flushInterval;
            }
        }, true);
        return printStream;
    }
    
    public static class JSONObject {
        
        private final HashMap<String, Object> map = new HashMap<>();
        
        void put(String key, Object value) {
            if (value != null){
                map.put(key, value);
            }
        }
        
        public String javaStringLiteral(String str) {
            StringBuilder sb = new StringBuilder("\"");
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '\n'){
                    sb.append("\\n");
                }else if (c == '\r'){
                    sb.append("\\r");
                }else if (c == '"'){
                    sb.append("\\\"");
                }else if (c == '\\'){
                    sb.append("\\\\");
                }else if (c < 0x20){
                    sb.append(String.format("\\%03o", (int) c));
                }else if (c >= 0x80){
                    sb.append(String.format("\\u%04x", (int) c));
                }else{
                    sb.append(c);
                }
            }
            sb.append("\"");
            return sb.toString();
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            Set<Map.Entry<String, Object>> entrySet = map.entrySet();
            builder.append("{");
            
            int i = 0;
            for (Map.Entry<String, Object> entry : entrySet) {
                Object val = entry.getValue();
                builder.append(quote(entry.getKey())).append(":");
                
                if (val instanceof String){
                    builder.append(javaStringLiteral((String) val));
                }else if (val instanceof Integer){
                    builder.append(Integer.valueOf(String.valueOf(val)));
                }else if (val instanceof Boolean){
                    builder.append(val);
                }else if (val instanceof JSONObject){
                    builder.append(val.toString());
                }else if (val.getClass().isArray()){
                    builder.append("[");
                    int len = Array.getLength(val);
                    for (int j = 0; j < len; j++) {
                        builder.append(Array.get(val, j).toString()).append(j != len - 1 ? "," : "");
                    }
                    builder.append("]");
                }
                
                builder.append(++i == entrySet.size() ? "}" : ",");
            }
            
            return builder.toString();
        }
        
        private String quote(String string) {
            return "\"" + string + "\"";
        }
    }
}

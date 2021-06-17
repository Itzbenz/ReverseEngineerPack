package Standalone.Server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

class Main {
    public static JarLoader lib;
    
    static {
        try {
            new WebServer();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws Throwable {
        args = System.getenv("args") == null ? args : System.getenv("args").split(":");
        String main = System.getenv("main");
        System.out.println("retard");
        Webhook.hook();
        if (main == null){
            while (true) {
                
                Thread.sleep(10000);
            }
        }
        if (main.trim().isEmpty()) return;
        System.out.println("Hacking time");
        lib = new JarLoader(new URL[]{}, ClassLoader.getSystemClassLoader());
        lib.addURL(System.getenv("urls").split(":"));
        Class<?> mainClass = lib.loadClass(main);
        Method m = mainClass.getMethod("main", String[].class);
        System.out.println("Initializing main method\nArguments:");
        System.out.println(Arrays.toString(args));
        m.invoke(null, (Object) args);//cast it to object because java is confused
        
    }
    
    public static class WebServer implements HttpHandler {
        public HttpServer server;
        public ExecutorService executorService = new ThreadPoolExecutor(0, 100, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            t.setPriority(2);
            return t;
        });
        public HashMap<String, String> redirect = new HashMap<>();
        public HashMap<String, Con<HttpExchange>> api = new HashMap<>();
        
        {
            redirect.put("/", "home.html");
            redirect.put("/favicon.ico", "favicon.png");
            addAPI("log", h -> sendResponse(h, "pog"));
        }
        
        
        public WebServer() throws IOException {
            registerAlive();
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 20);
            server.createContext("/", this);
            server.setExecutor(executorService);
            server.start();
        }
        
        public static void registerAlive() {
            String s = System.getenv("pings");
            if (s == null || s.isEmpty()) return;
            ArrayList<URL> ping = new ArrayList<>();
            for (String ss : s.split(" ")) {
                try {
                    ss = ss + (ss.endsWith("/") ? "" : "/") + "assad";
                    ping.add(new URL(ss));
                    System.out.println("Registered: " + ss);
                }catch(MalformedURLException e){
                    e.printStackTrace();
                }
            }
            
            ScheduledExecutorService schedule = Executors.newScheduledThreadPool(5, r -> {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                t.setPriority(1);
                return t;
            });
            for (URL u : ping) {
                schedule.schedule(() -> {
                    try {
                        u.openStream().readAllBytes();
                    }catch(Exception e){
                        System.err.println(e);
                    }
                }, 30, TimeUnit.SECONDS);
            }
        }
        
        public static void sendResponse(HttpExchange exchange, String data) throws IOException {
            data += "\n";
            exchange.sendResponseHeaders(200, data.length());
            exchange.getResponseBody().write(data.getBytes());
            exchange.getResponseBody().flush();
        }
        
        public void fillThisBoringHeader(Headers headers) {
            headers.add("Status", "200");
            headers.add("Date", getServerTime());
            headers.add("Content-type", "text/text");
        }
        
        public String getServerTime() {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            return dateFormat.format(calendar.getTime());
        }
        
        public void api(String res, HttpExchange exchange) throws IOException {
            if (!api.containsKey(res)){
                sendResponse(exchange, "API not found");
                return;
            }
            try {
                api.get(res).accept(exchange);
            }catch(Exception t){
                sendResponse(exchange, t.getClass().getSimpleName() + t.getMessage());
                
            }
        }
        
        
        public void sendResource(String res, HttpExchange exchange) throws IOException {
            
            InputStream is = get(res);
            int status = 200;
            if (is == null){
                is = get(res + ".html");
                if (is == null){
                    is = get("404.html");
                    status = 404;
                }
            }
            
            byte[] data;
            if (is == null){
                data = (getServerTime() + ": 404").getBytes(StandardCharsets.UTF_8);
            }else{
                data = is.readAllBytes();
            }
            exchange.sendResponseHeaders(status, data.length);
            exchange.getResponseBody().write(data);
            exchange.getResponseBody().flush();
            
        }
        
        public InputStream get(String res) {
            try {
                return new FileInputStream(res);
            }catch(FileNotFoundException e){
                return null;
            }
        }
        
        public void addAPI(String name, Con<HttpExchange> h) {
            api.put("/api/" + name, h);
        }
        
        @Override
        public void handle(HttpExchange exchange) {
            
            //Log.info("Server", "A new " + exchange.getRequestMethod() + " just appeared");
            //Log.info("Server", "URL: " + exchange.getRequestURI());
            //Log.info("Server", "Body: " + new String(exchange.getRequestBody().readAllBytes()));
            //fillThisBoringHeader(exchange.getResponseHeaders());
            
            try {
                String resource = exchange.getRequestURI().getPath();
                if (exchange.getRequestMethod().equals("HEAD")){
                    exchange.sendResponseHeaders(200, 0);
                    return;
                }else if (resource.startsWith("/api/")){
                    api(resource, exchange);
                }else{
                    if (redirect.containsKey(exchange.getRequestURI().getPath())) resource = redirect.get(exchange.getRequestURI().getPath());
                    else resource = resource.substring(1);
                    sendResource(resource, exchange);
                }
                System.out.println(exchange.getRequestMethod() + ":" + exchange.getRequestURI().toString() + ", " + resource);
            }catch(Exception e){
                System.out.println(exchange.getRequestMethod() + ":" + exchange.getRequestURI().toString() + ", " + e);
                e.printStackTrace();
                byte[] data = e.getMessage().getBytes(StandardCharsets.UTF_8);
                try {
                    exchange.sendResponseHeaders(500, data.length);
                    exchange.getResponseBody().write(data);
                    exchange.getResponseBody().flush();
                }catch(Exception ignored){}
            }finally{
                exchange.close();
            }
        }
        
        public interface Con<T> {
            void accept(T t) throws Exception;
        }
    }
    
    public static class JarLoader extends URLClassLoader {
        static {
            registerAsParallelCapable();
        }
        
        public JarLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
        
        public JarLoader(URL[] urls) {
            super(urls);
        }
        
        
        public void addURL(String... urls) throws MalformedURLException {
            for (String s : urls)
                addURL(new URL(s));
        }
        
        @Override
        public void addURL(URL jar) {
            
            try {
                
                File temp = new File(System.getProperty("java.io.tmpdir"), jar.getFile());
                if (!temp.getParentFile().exists()) System.out.println(temp.getAbsolutePath() + ": " + temp.getParentFile().mkdirs());
                if (!temp.exists()){
                    System.out.println("Downloading: " + jar);
                    FileOutputStream o = new FileOutputStream(temp);
                    o.write(jar.openStream().readAllBytes());
                    o.close();
                }
                jar = temp.toURI().toURL();
            }catch(Exception e){
                e.printStackTrace();
            }
            System.out.println("Adding: " + jar);
            super.addURL(jar);
        }
        
        
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return super.loadClass(name);
        }
    }
    
    public static class Webhook {
        final static Webhook h = new Webhook();
        public static URL realUrl = null;
        static String url = System.getenv("DiscordWebhook");
        static JSONObject json = new JSONObject();
        static ExecutorService executorService = Executors.newCachedThreadPool();
        
        static {
            h.username = "o7Fire Replit Java API";
            h.content = "h";
        }
        
        static {
            try {
                realUrl = new URL(url);
            }catch(MalformedURLException e){
                e.printStackTrace();
            }
        }
        
        public String content = "", username, avatar_url;
        
        public static void post(String dat) {
            h.content = dat;
            executorService.submit((Runnable) Webhook::post);
        }
        
        public static void post() {
            try {
                // build connection
                HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
                // set request properties
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
                // enable output and input
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                String doAnother = "";
                if (h.content.length() > 1920){
                    doAnother = h.content.substring(1900);
                    h.content = h.content.substring(0, 1900);
                }
                h.content = "```java\n" + h.content + "\n```";
                
                
                json.put("content", h.content);
                json.put("username", h.username);
                json.put("avatar_url", h.avatar_url);
                conn.getOutputStream().write(json.toString().getBytes(StandardCharsets.UTF_8));
                conn.getOutputStream().flush();
                conn.getOutputStream().close();
                conn.getInputStream().close();
                conn.disconnect();
                if (!doAnother.isEmpty()){
                    h.content = doAnother;
                    post();
                }
            }catch(Exception e){
                e.printStackTrace();
                System.out.println(e);
                System.out.println(json.toString());
            }
        }
        
        
        public static void hook() {
            if (url == null) return;
            long lastFlush = System.currentTimeMillis();
            final long[] nextFlush = {lastFlush + 4000};
            //PrintStream out = System.out;
            System.setErr(new PrintStream(new OutputStream() {
                StringBuilder sb = new StringBuilder();
                
                @Override
                public void write(int b) throws IOException {
                    sb.append((char) b);
                }
                
                @Override
                public void flush() throws IOException {
                    if (nextFlush[0] > System.currentTimeMillis()) return;
                    Webhook.post(sb.toString());
                    sb = new StringBuilder();
                    nextFlush[0] = System.currentTimeMillis() + 4000;
                }
            }, true));
            //System.out.println(Utility.getDate());
            System.err.println("Patch");
            System.out.println("PAtch");
        }
    }
    
    private static class JSONObject {
        
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

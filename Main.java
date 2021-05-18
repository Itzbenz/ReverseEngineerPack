import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Main {
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
                    ss = (ss.endsWith("/") ? "" : "/") + "assad";
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
                    }catch(Exception ignored){
                    }
                }, 160, TimeUnit.SECONDS);
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
                    if (redirect.containsKey(exchange.getRequestURI().getPath()))
                        resource = redirect.get(exchange.getRequestURI().getPath());
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
                if (!temp.getParentFile().exists())
                    System.out.println(temp.getAbsolutePath() + ": " + temp.getParentFile().mkdirs());
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
}
